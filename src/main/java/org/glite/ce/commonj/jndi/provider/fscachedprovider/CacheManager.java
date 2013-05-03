/*
 * Copyright (c) Members of the EGEE Collaboration. 2004. 
 * See http://www.eu-egee.org/partners/ for details on the copyright
 * holders.  
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 *
 *     http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

/*
 * 
 * Authors: Paolo Andreetto, <paolo.andreetto@pd.infn.it>
 *
 */

package org.glite.ce.commonj.jndi.provider.fscachedprovider;

import java.io.File;
import java.io.FilenameFilter;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import javax.naming.Name;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.CommunicationException;
import javax.naming.NameAlreadyBoundException;
import javax.naming.OperationNotSupportedException;
import javax.naming.NameNotFoundException;
import javax.naming.NotContextException;
import javax.naming.directory.Attributes;

import org.glite.ce.commonj.CEResource;

import org.apache.log4j.Logger;

public class CacheManager {
	private static Logger logger = Logger.getLogger(CacheManager.class.getName());

	public static String ROOT_ATTRS_FILE = "..root-attributes";
	public final static int MUST_BE_OBJECT = 0;
	public final static int MUST_BE_CONTEXT = 1;

	protected String absoluteBase;
	protected ContextCache objCache;
	protected ContextCache attrCache;
	protected CacheLock mutex;

	public CacheManager(String base) {
		absoluteBase = base;
		objCache = new ContextCache("OBJECT CACHE");
		attrCache = new ContextCache("ATTRIBUTE CACHE");
		mutex = new CacheLock();
	}

	public CacheManager(String base, int cacheSize) {
		this(base);
		objCache.setCacheMaxSize(cacheSize);
		attrCache.setCacheMaxSize(cacheSize);
	}

	public Object getObject(Name name) throws NamingException {
		mutex.lock(name, CacheLock.READ_OPERATION);

		String oName = name.toString();
		Object result = objCache.lookup(oName);
        try {
            if (result == null) {
				File file = new File(absoluteBase + File.separator + oName);
				if (!file.exists())
					throw new NameNotFoundException(oName);

				if (file.isDirectory()) {
					result = file;
				} else {
					result = retrieveObject(absoluteBase + File.separator
							+ oName);

					if (result instanceof CEResource) {
						try {
							objCache.load(oName, (CEResource) result);
						} catch (Exception ex) {
							logger.debug(ex.getMessage(), ex);
						}
					}
				}
            }
        } finally {
            mutex.unlock(name, CacheLock.READ_OPERATION);
        }

		return result;
	}

	public Attributes getAttributes(Name name) throws NamingException {
		mutex.lock(name, CacheLock.READ_OPERATION);

		String bName = getBindingsName(name);
		Attributes loadedAttrs = (Attributes) attrCache.lookup(bName);
        try {
            if (loadedAttrs == null) {
				loadedAttrs = (Attributes) retrieveObject(absoluteBase
						+ File.separator + bName);
				try {
					attrCache.load(bName, (CEResource) loadedAttrs);
				} catch (Exception ex) {
					logger.debug(ex.getMessage(), ex);
				}
            }
        } finally {
            mutex.unlock(name, CacheLock.READ_OPERATION);
        }

		return loadedAttrs;
	}

	public ArrayList getAttributeList(Name name) throws NamingException {
		mutex.lock(name, CacheLock.LIST_OPERATION);

		ArrayList list = null;

		try {
			String oName = name.toString();
			File file = new File(absoluteBase + File.separator + oName);
			if (!file.exists())
				throw new NameNotFoundException(oName);

			if (!file.isDirectory())
				throw new NotContextException(oName);

			String[] tmpl = file.list(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.startsWith(".") && !name.startsWith("..");
				}
			});

                        if(tmpl == null) {
                            throw new NamingException("getAttributeList error: the file.list(filter) returned null list. This problem occurs when the pathname (" + absoluteBase + File.separator + oName + ") does not denote a directory, or I/O error occured");
                        }

			list = new ArrayList(tmpl.length);
			for (int k = 0; k < tmpl.length; k++) {
				Name itemName = ((Name) name.clone()).add(tmpl[k].substring(1));
				Object[] pair = new Object[] { itemName, getAttributes(itemName) };
				list.add(pair);
			}
		} finally {
			mutex.unlock(name, CacheLock.LIST_OPERATION);
		}

		return list;
	}

	public void putObject(Name name, Object obj, Attributes attrs, boolean overwriteObj) throws NamingException {
		mutex.lock(name, CacheLock.WRITE_OPERATION);

        boolean cannotPutObj = false;
        boolean cannotPutAttr = false;
        String bName = getBindingsName(name);
        File objFile = new File(absoluteBase + File.separator + name);

		try {
			if (attrs != null) {
                cannotPutAttr = true;
				storeObject(absoluteBase + File.separator + bName, attrs);
                cannotPutAttr = false;
				updateCache(bName, attrs, overwriteObj);
			}

			if (obj != null && !(obj instanceof Context)) {
				if (!overwriteObj && objFile.exists())
					throw new NameAlreadyBoundException("Object already exists " + name);
				if (overwriteObj && !objFile.exists())
					throw new NameAlreadyBoundException("Object doesn't exist " + name);

                cannotPutObj = true;
				storeObject(absoluteBase + File.separator + name, obj);
                cannotPutObj = false;
				updateCache(name.toString(), obj, overwriteObj);
			}

		} finally {

            if( cannotPutAttr || cannotPutObj ){
                attrCache.unload(bName);
                File attrFile = new File(absoluteBase + File.separator + bName);
                if( !attrFile.delete() )
                    logger.warn("Cannot remove corrupted file: " + attrFile.getAbsolutePath());
            }

            if( cannotPutObj ){
                objCache.unload(name.toString());
                if( !objFile.delete() )
                    logger.warn("Cannot remove corrupted file: " + objFile.getAbsolutePath());
            }

			mutex.unlock(name, CacheLock.WRITE_OPERATION);
		}
	}

	public ArrayList recursiveRemove(Name name, int checkDir) throws NamingException {
		mutex.lock(name, CacheLock.DELETE_OPERATION);

		ArrayList result = new ArrayList(0);

		try {
			String oName = name.toString();
			File file = new File(absoluteBase + File.separator + oName);

			if (!file.exists())
				throw new NameNotFoundException(oName);
			if (file.isDirectory() && checkDir == MUST_BE_OBJECT)
				throw new NamingException("Cannot unbind the context " + oName);
			if (!file.isDirectory() && checkDir == MUST_BE_CONTEXT)
				throw new NotContextException(oName);

			removeObject(name, result);

		} finally {
			mutex.unlock(name, CacheLock.DELETE_OPERATION);
		}

		return result;
	}

	private void removeObject(Name name, ArrayList buffer) throws NamingException {
		String oName = name.toString();
		String bName = getBindingsName(name);
        
		Object oldObj = null;
        Object oldAttrs = null;

		File file = new File(absoluteBase + File.separator + oName);
		if (file.isDirectory()) {
			String[] list = file.list(new FilenameFilter() {
				public boolean accept(File dir, String fName) {
					return !fName.startsWith(".");
				}
			});

			for (int k = 0; k < list.length; k++) {
				Name itemName = ((Name) name.clone()).add(list[k]);
				removeObject(itemName, buffer);
			}

			oldObj = new String();
            oldAttrs = attrCache.lookup(bName);

		} else {
			oldObj = objCache.lookup(oName);
            oldAttrs = attrCache.lookup(bName);
			
			if (oldObj == null) {
				try {
					oldObj = retrieveObject(absoluteBase + File.separator
							+ oName);
				} catch (CommunicationException cEx) {
					logger.debug("Spurious file " + oName);
				}
			}
		}

		objCache.unload(oName);
		attrCache.unload(bName);

		if (!file.delete())
			throw new NamingException("Cannot delete " + file.getAbsolutePath());

		File bFile = new File(absoluteBase + File.separator + bName);
		if (bFile.exists() && !bFile.delete())
			throw new NamingException("Cannot delete "
					+ bFile.getAbsolutePath());
        
		buffer.add(new Object[] { name, oldObj, oldAttrs });
	}

	public void createContext(Name name, Attributes attrs) throws NamingException {
		String oName = name.toString();
		File file = new File(absoluteBase + File.separator + oName);

		if (file.isDirectory()) {
			getAttributes(name);
		} else {
			mutex.lock(name, CacheLock.WRITE_OPERATION);

            String bName = getBindingsName(name);
            boolean cannotCreate = true;
			try {
				if (file.mkdir() && attrs != null) {
					storeObject(absoluteBase + File.separator + bName, attrs);
                    cannotCreate = false;
					updateCache(bName, attrs, false);
				} else {
					throw new NamingException("Cannot create subcontext " + oName);
			    }
			} finally {
                if( cannotCreate ){
                    file.delete();
                    (new File(bName)).delete();
                }
				mutex.unlock(name, CacheLock.WRITE_OPERATION);
			}
		}        
	}

	public void renameObject(Name srcName, Name trgName) throws NamingException {
		String source = srcName.toString();
		String bSource = getBindingsName(srcName);
		File sourceFile = new File(absoluteBase + File.separator + source);
		File bSourceFile = new File(absoluteBase + File.separator + bSource);

		String target = trgName.toString();
		String bTarget = getBindingsName(trgName);
		File targetFile = new File(absoluteBase + File.separator + target);
		File bTargetFile = new File(absoluteBase + File.separator + bTarget);

		mutex.lock(srcName, CacheLock.DELETE_OPERATION);
		mutex.lock(trgName, CacheLock.WRITE_OPERATION);

		try {
			if (!sourceFile.getParent().equals(targetFile.getParent())) {
				throw new OperationNotSupportedException(
						"Do not support rename across different contexts");
			}

			if (!bSourceFile.renameTo(bTargetFile)) {
				throw new NamingException("Cannot rename " + source);
			}

			if (!sourceFile.renameTo(targetFile)) {
				bTargetFile.renameTo(bSourceFile);
				throw new NamingException("Cannot rename " + source);
			}
						
			CEResource tmpRes = objCache.lookup(source);
			CEResource tmpBinds = attrCache.lookup(bSource);
			
			try {
				if (tmpRes != null) {
					objCache.unload(source);
					objCache.load(target, tmpRes);
				}
				if (tmpBinds != null) {
					attrCache.unload(bSource);
					attrCache.load(bTarget, tmpBinds);
				}
			} catch (Exception ex) {
				logger.debug(ex.getMessage(), ex);
			}

		} finally {
			mutex.unlock(srcName, CacheLock.DELETE_OPERATION);
			mutex.unlock(trgName, CacheLock.WRITE_OPERATION);
		}
	}

	protected Object retrieveObject(String filename) throws NamingException {
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		Object obj = null;

		try {
			fis = new FileInputStream(filename);
			ois = new ObjectInputStream(fis);
			obj = ois.readObject();
		} catch (Exception ex) {
			logger.debug("retrieveObject: cannot retrieve the file \"" + filename + "\"\n" + ex.getMessage());
			throw new NamingException(ex.getMessage());
		} finally {
			if (ois != null)
				try {					
					ois.close();
				} catch (Exception ex) {}
			if (fis != null)
				try {
					fis.close();
				} catch (Exception ex) {}
		}

		return obj;
	}

	protected void storeObject(String fileName, Object obj) throws NamingException {
		if (obj == null) {
			return;
		}

		FileOutputStream fos = null;
		ObjectOutputStream oos = null;
        
        try {
			fos = new FileOutputStream(fileName);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(obj);
        } catch (FileNotFoundException e) {
            //logger.error("storeObject error: fileName " + fileName + "\n" + ioEx.getMessage());
            
            throw new NamingException(e.getMessage());
        } catch (IOException ioEx) {
//			 //logger.error("storeObject error: fileName " + fileName + "\n" + ioEx.getMessage());
  
            throw new NamingException(ioEx.getMessage());
		} finally {
			if (oos != null) {
				try {
					oos.flush();
					oos.close();
				} catch (Exception ex) {		
					logger.error("storeObject error: " + ex.getMessage());
				}
			}
			
			if (fos != null) {
				try {
					fos.flush();
					fos.close();
				} catch (Exception ex) {
					logger.error("storeObject error: " + ex.getMessage());
				}
			}
		}
	}

	private void updateCache(String oName, Object obj, boolean overwrite) {
		if (obj == null || !(obj instanceof CEResource) || oName == null) {
			return;
		}
		
		ContextCache cache = obj instanceof Attributes? attrCache: objCache;
        
        if(cache == null) {
            return;
        }
        
		try {
            if( overwrite ) {
                try {
                    cache.update(oName, (CEResource) obj);
                } catch(Exception e) {
                    cache.load(oName, (CEResource) obj);
                }                
            } else {
                cache.load(oName, (CEResource) obj);
            }
		} catch (Exception ex) {
			logger.debug(ex.getMessage());
		}
	}

	protected String getBindingsName(Name name) throws NamingException {
		Name result = (Name) name.clone();
		if (name.size() == 0) {
			result.add(ROOT_ATTRS_FILE);
		} else {
			result.remove(result.size() - 1);
			result.add("." + name.get(name.size() - 1));
		}
		return result.toString();
	}

	
	
}
