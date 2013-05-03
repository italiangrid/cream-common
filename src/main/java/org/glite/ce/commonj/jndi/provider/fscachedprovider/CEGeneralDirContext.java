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
 *          Luigi Zangrando, <luigi.zangrando@pd.infn.it>
 *
 */


package org.glite.ce.commonj.jndi.provider.fscachedprovider;

import java.io.File;
import java.io.IOException;
import java.io.FilenameFilter;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.CompoundName;
import javax.naming.CompositeName;
import javax.naming.Binding;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NameNotFoundException;
import javax.naming.NotContextException;
import javax.naming.InvalidNameException;
import javax.naming.NameAlreadyBoundException;
import javax.naming.OperationNotSupportedException;
import javax.naming.CommunicationException;
import javax.naming.NameParser;
import javax.naming.NameClassPair;
import javax.naming.spi.DirectoryManager;
import javax.naming.spi.DirStateFactory;
import javax.naming.directory.DirContext;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.AttributeModificationException;
import javax.naming.directory.SearchResult;
import javax.naming.event.EventDirContext;
import javax.naming.event.NamingListener;
import javax.naming.event.NamespaceChangeListener;
import javax.naming.event.ObjectChangeListener;
import javax.naming.event.NamingEvent;

import org.apache.log4j.Logger;
import org.glite.ce.commonj.CEResource;

public class CEGeneralDirContext implements DirContext, CEResource, EventDirContext {

    private static Logger logger = Logger.getLogger(CEGeneralDirContext.class.getName());

    protected final static NameParser nameParser = new CEGeneralNameParser();
    protected static Hashtable eventManagerTable = null;
    protected static Hashtable cacheManagerTable = null;

    protected CEEventManager eventManager;
    protected CacheManager cacheManager;
    protected Hashtable environment;
    protected Name contextName;
    protected String absoluteBase = null;

    public static String CACHE_SIZE_LABEL = "cache.size";
    public static String CNAME_ATTR_LABEL = "classname";
    public static String MODTIME_ATTR_LABEL = "modificationtime";

    protected static String checkAbsolutePath(String docBase) throws NamingException{
        if(docBase == null ) {
            throw new NamingException("Missing docBase or contextPath");
        }

        try {
            File base = new File(docBase).getCanonicalFile();
            if(!base.exists() || !base.isDirectory() || !base.canRead()) {
                throw new NamingException("Bad docBase");
            }

            return base.getAbsolutePath();

        } catch (IOException ioEx) {
            throw new NamingException(ioEx.getMessage());
        }
    }

    protected CEGeneralDirContext(String docBase, Name ctxPath, Hashtable inEnv) {
        environment = (inEnv != null) ? (Hashtable) (inEnv.clone()) : null;
        absoluteBase = docBase;
        contextName = (Name) ctxPath.clone();

        if( eventManagerTable==null ){
            eventManagerTable = new Hashtable(0);
        }
        eventManager = (CEEventManager)eventManagerTable.get(absoluteBase);
        if( eventManager==null ){
            eventManager = new CEEventManager();
            eventManagerTable.put(absoluteBase, eventManager);
        }

        if( cacheManagerTable==null ){
            cacheManagerTable = new Hashtable(0);
        }
        cacheManager = (CacheManager)cacheManagerTable.get(absoluteBase);
        if( cacheManager==null ){
            Object tmpo = environment.get(CACHE_SIZE_LABEL);
            if(tmpo != null && (tmpo instanceof Integer)) {
                cacheManager = new CacheManager(absoluteBase, ((Integer) tmpo).intValue());
            }else{
                cacheManager = new CacheManager(absoluteBase);
            }
            cacheManagerTable.put(absoluteBase, cacheManager);
        }
    }

    protected CEGeneralDirContext(String docBase, String ctxPath, Hashtable inEnv) throws NamingException {
        this(docBase, nameParser.parse(ctxPath), inEnv);
    }

    public CEGeneralDirContext(String docBase, Hashtable inEnv) throws NamingException {
        this(checkAbsolutePath(docBase), "", inEnv);

        try{
            cacheManager.getAttributes(contextName);
        }catch(NamingException nEx){
            CEGeneralAttributes rootAttrs = new CEGeneralAttributes(CEGeneralDirContext.MODTIME_ATTR_LABEL,
                                                                    new Long(System.currentTimeMillis()));
            cacheManager.putObject(contextName, null, rootAttrs,false);
        }
    }

    protected Context cloneCtx() {
        return new CEGeneralDirContext(absoluteBase, contextName, environment);
    }

    public Object clone() {
        return cloneCtx();
    }

    public String getAbsoluteBase() {
        return absoluteBase;
    }

    protected Name getCompleteName(Name name) throws NamingException {
        Name result = (Name) contextName.clone();
        if(name instanceof CompositeName) {
            if(name.size() > 1) {
                throw new InvalidNameException(name.toString() + " has more components than namespace can handle");
            }

            result.addAll(nameParser.parse(name.get(0)));
        } else {
            result.addAll(name);
        }

        for (int k = 0; k < result.size(); k++) {
            if(result.get(k).startsWith("."))
                throw new NamingException("Hidden files are not allowed");
        }

        return result;
    }

    public Object lookup(String name) throws NamingException {
        return internalLookup(nameParser.parse(name), false);
    }

    public Object lookup(Name name) throws NamingException {
        return internalLookup(name, false);
    }

    protected Object internalLookup(Name name, boolean isAbsolute) throws NamingException {
        if(name.isEmpty()) {
            return cloneCtx();
        }

        Name targetName = isAbsolute ? name : getCompleteName(name);

        Object result = cacheManager.getObject(targetName);
        if( result instanceof File ){
            return new CEGeneralDirContext(absoluteBase, targetName, environment);
        }

        try {
            return DirectoryManager.getObjectInstance(result, name, this, environment, null);
        } catch (Exception ex) {
            throw new NamingException(ex.getMessage());
        }
    }



    public void bind(String name, Object obj) throws NamingException {
        internalBind(nameParser.parse(name), obj, null, false);
    }

    public void bind(String name, Object obj, Attributes attrs) throws NamingException {
        internalBind(nameParser.parse(name), obj, attrs, false);
    }

    public void bind(Name name, Object obj) throws NamingException {
        internalBind(name, obj, null, false);
    }

    public void bind(Name name, Object obj, Attributes attrs) throws NamingException {
        internalBind(name, obj, attrs, false);
    }



    protected void internalBind(Name name, Object obj, Attributes attrs, boolean allowRebind) throws NamingException {
        if(name.isEmpty())
            throw new InvalidNameException("Cannot bind empty name");

        if(obj == null)
            throw new NamingException("Object cannot be null");

        if(obj instanceof Context)
            throw new NamingException("Cannot bind a directory");

        Name targetName = getCompleteName(name);

        if(attrs == null || !(attrs instanceof CEGeneralAttributes)) {
            attrs = new CEGeneralAttributes(attrs);
        }

        attrs.put(CNAME_ATTR_LABEL, obj.getClass().getName());
        attrs.put(MODTIME_ATTR_LABEL, new Long(System.currentTimeMillis()));

        DirStateFactory.Result newObjRes = DirectoryManager.getStateToBind(obj, name, this, environment, attrs);

        cacheManager.putObject(targetName, newObjRes.getObject(), newObjRes.getAttributes(), allowRebind);


        if(!allowRebind) {
            fireEvents(targetName, NamingEvent.OBJECT_ADDED, obj, null, null);
        } else {
            fireEvents(targetName, NamingEvent.OBJECT_CHANGED, obj, null, null);
        }
    }



    public void rebind(String name, Object obj) throws NamingException {
        internalBind(nameParser.parse(name), obj, null, true);
    }

    public void rebind(String name, Object obj, Attributes attrs) throws NamingException {
        internalBind(nameParser.parse(name), obj, attrs, true);
    }

    public void rebind(Name name, Object obj) throws NamingException {
        internalBind(name, obj, null, true);
    }

    public void rebind(Name name, Object obj, Attributes attrs) throws NamingException {
        internalBind(name, obj, attrs, true);
    }

    public void unbind(String name) throws NamingException {
        unbind(nameParser.parse(name));
    }

    public void unbind(Name name) throws NamingException {
        if(name.isEmpty()) {
            throw new InvalidNameException("Cannot unbind empty name");
        }

        Name target = getCompleteName(name);
        try {
    	    ArrayList evnList = cacheManager.recursiveRemove(target, CacheManager.MUST_BE_OBJECT);
            for(int k=0; k< evnList.size(); k++){
                Object[] evnPair = (Object[])evnList.get(k);
                if( evnPair[1]!=null )
                    fireEvents((Name)evnPair[0], NamingEvent.OBJECT_REMOVED, null, evnPair[1], null);
            }
	} catch(NameNotFoundException e) {}

        eventManager.removeAll(target);
    }



    public void rename(String source, String target) throws NamingException {
        rename(nameParser.parse(source), nameParser.parse(target));
    }



    public void rename(Name source, Name target) throws NamingException {
        if(source.isEmpty() || target.isEmpty()) {
            throw new InvalidNameException("Cannot rename empty name");
        }

        Name srcTarget = getCompleteName(source);
        Name dstTarget = getCompleteName(target);

        cacheManager.renameObject(srcTarget, dstTarget);

        fireEvents(srcTarget, NamingEvent.OBJECT_RENAMED, null, null, dstTarget);
    }



    public NamingEnumeration list(String name) throws NamingException {
        return list(nameParser.parse(name));
    }



    public NamingEnumeration list(Name name) throws NamingException {
        ArrayList attrList = cacheManager.getAttributeList(getCompleteName(name));
        ArrayList list = new ArrayList(attrList.size());
        for(int k=0; k<attrList.size(); k++) {
            Object[] pair = (Object[])attrList.get(k);

            Name itemName = (Name)pair[0];
            Attributes attrs = (Attributes)pair[1];
            list.add(new NameClassPair(itemName.get(itemName.size()-1), (String)attrs.get(CNAME_ATTR_LABEL).get(0)));
        }

        return new CEGeneralEnumeration(list);
    }



    public NamingEnumeration listBindings(String name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public NamingEnumeration listBindings(Name name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public void destroySubcontext(String name) throws NamingException {
        destroySubcontext(nameParser.parse(name));
    }

    public void destroySubcontext(Name name) throws NamingException {
        if(name.isEmpty()) {
            throw new InvalidNameException("Cannot destroy context using empty name");
        }

        Name target = getCompleteName(name);
        ArrayList evnList = cacheManager.recursiveRemove(target, CacheManager.MUST_BE_CONTEXT);
        
        for(int k=0; k< evnList.size(); k++){
            Object[] evnPair = (Object[])evnList.get(k);
            if( evnPair.length == 3 ) {
                fireEvents((Name)evnPair[0], NamingEvent.OBJECT_REMOVED, null, evnPair[1], evnPair[2]);
            }
        }

        eventManager.removeAll(target);
    }

    public Context createSubcontext(String name) throws NamingException {
        return createSubcontext(nameParser.parse(name), null);
    }

    public DirContext createSubcontext(String name, Attributes attrs) throws NamingException {
        return createSubcontext(nameParser.parse(name), attrs);
    }

    public Context createSubcontext(Name name) throws NamingException {
        return createSubcontext(name, null);
    }

    public DirContext createSubcontext(Name name, Attributes attrs) throws NameAlreadyBoundException, NamingException {
        if(name.isEmpty()) {
            throw new InvalidNameException("Cannot bind empty name");
        }

        Name targetName = getCompleteName(name);

        CEGeneralDirContext subCtx = null;

        if(attrs == null || !(attrs instanceof CEGeneralAttributes)) {
            attrs = new CEGeneralAttributes(attrs);
        }

        attrs.put(CNAME_ATTR_LABEL, this.getClass().getName());
        attrs.put(MODTIME_ATTR_LABEL, new Long(System.currentTimeMillis()));

        Enumeration tokens = targetName.getSuffix(contextName.size()).getAll();
        Name tmpName = (Name)contextName.clone();
        while(tokens.hasMoreElements()) {
            tmpName.add((String)tokens.nextElement());

            try{
                cacheManager.createContext(tmpName, attrs);
            }catch(NameAlreadyBoundException nabEx){
                logger.debug(nabEx.getMessage());
            }

            subCtx = new CEGeneralDirContext(absoluteBase, tmpName, environment);

            fireEvents(tmpName, NamingEvent.OBJECT_ADDED, subCtx.clone(), null, null);
        }

        return subCtx;

    }

    public Object lookupLink(String name) throws NamingException {
        return lookup(nameParser.parse(name));
    }

    public Object lookupLink(Name name) throws NamingException {
        return lookup(name);
    }

    public NameParser getNameParser(String name) throws NamingException {
        return nameParser;
    }

    public NameParser getNameParser(Name name) throws NamingException {
        return nameParser;
    }

    public String composeName(String name, String prefix) throws NamingException {
        if(prefix.endsWith(File.separator))
            return prefix + name;
        return prefix + File.separator + name;
    }

    public Name composeName(Name name, Name prefix) throws NamingException {
        String tmps = prefix.toString() + File.separator + name.toString();
        return nameParser.parse(tmps);
    }

    public Object addToEnvironment(String propName, Object propVal) throws NamingException {
        if(environment == null) {
            environment = new Hashtable(5, 0.75f);
        }
        return environment.put(propName, propVal);
    }

    public Object removeFromEnvironment(String propName) throws NamingException {
        if(environment == null)
            return null;

        return environment.remove(propName);
    }

    public Hashtable getEnvironment() throws NamingException {
        if(environment == null) {
            return new Hashtable(3, 0.75f);
        } else {
            return (Hashtable) environment.clone();
        }
    }

    public String getNameInNamespace() {
        return contextName.toString();
    }

    public Attributes getAttributes(String name) throws NamingException {
        return getAttributes(nameParser.parse(name), null);
    }

    public Attributes getAttributes(Name name) throws NamingException {
        return getAttributes(name, null);
    }

    public Attributes getAttributes(String name, String[] ids) throws NamingException {
        return getAttributes(nameParser.parse(name), ids);
    }

    public Attributes getAttributes(Name name, String[] ids) throws NamingException {
        Attributes loadedAttrs = cacheManager.getAttributes(getCompleteName(name));

        if(ids == null)
            return loadedAttrs;

        Attributes result = new CEGeneralAttributes();
        for (int k = 0; k < ids.length; k++) {
            Attribute attr = loadedAttrs.get(ids[k]);
            if(attr != null)
                result.put(attr);
        }

        return result;
    }

    
    public void modifyAttributes(String name, int modOp, Attributes attrs) throws NamingException {
        modifyAttributes(nameParser.parse(name), modOp, attrs);
    }

    
    public void modifyAttributes(Name name, int modOp, Attributes attrs) throws NamingException {
        ModificationItem[] mods = new ModificationItem[attrs.size()];
        Enumeration allAttrs = attrs.getAll();
        for (int r = 0; allAttrs.hasMoreElements(); r++) {
            Attribute attr = (Attribute) allAttrs.nextElement();
            mods[r] = new ModificationItem(modOp, attr);
        }
        modifyAttributes(name, mods);
   }


    public void modifyAttributes(Name name, ModificationItem[] mods) throws NamingException {
        Name targetName = getCompleteName(name);
        CEGeneralAttributes modAttrs = (CEGeneralAttributes)cacheManager.getAttributes(targetName);
        CEGeneralAttributes newAttributes = new CEGeneralAttributes();
        CEGeneralAttributes oldAttributes = new CEGeneralAttributes();

        for (int k = 0; k < mods.length; k++) {
            Attribute newAttr = mods[k].getAttribute();
            if(newAttr.getID().equals(CNAME_ATTR_LABEL))
                continue;

            NamingEnumeration values = newAttr.getAll();
            Attribute oldAttr = modAttrs.get(newAttr.getID());
                
            switch(mods[k].getModificationOp()) {
            case DirContext.ADD_ATTRIBUTE:
                if(oldAttr == null) {
                    modAttrs.put(newAttr);
                    newAttributes.put(newAttr);
                } else {
                    while(values.hasMoreElements()) {
                        oldAttr.add(values.nextElement());
                    }

                    modAttrs.put(oldAttr);
                    newAttributes.put((Attribute)oldAttr.clone());
                }
                break;

            case DirContext.REPLACE_ATTRIBUTE:
                if(oldAttr == null)
                    throw new AttributeModificationException(newAttr.getID());

                modAttrs.put(newAttr);
                newAttributes.put(newAttr);
                oldAttributes.put(oldAttr);
                break;

            case DirContext.REMOVE_ATTRIBUTE:
                if(oldAttr != null) {
                    while(values.hasMoreElements()) {
                        oldAttr.remove(values.nextElement());
                    }
                    if(oldAttr.size() == 0) {
                        modAttrs.remove(newAttr.getID());
                    } else {
                        modAttrs.put(oldAttr);
                    }

                    oldAttributes.put(oldAttr);
                }
            }
        }

        modAttrs.put(MODTIME_ATTR_LABEL, new Long(System.currentTimeMillis()));
        cacheManager.putObject(targetName, null, modAttrs, true);   
        fireEvents(targetName, NamingEvent.OBJECT_CHANGED, newAttributes, oldAttributes, null);
    }



    public void modifyAttributes(String name, ModificationItem[] mods) throws NamingException {
        modifyAttributes(nameParser.parse(name), mods);
    }

    public DirContext getSchema(Name name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public DirContext getSchema(String name) throws NamingException {
        return getSchema(nameParser.parse(name));
    }

    public DirContext getSchemaClassDefinition(Name name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public DirContext getSchemaClassDefinition(String name) throws NamingException {
        return getSchemaClassDefinition(nameParser.parse(name));
    }

    public NamingEnumeration search(Name name, Attributes matchingAttributes, String[] attributesToReturn) 
        throws NamingException {

        ArrayList attrList = cacheManager.getAttributeList(getCompleteName(name));

        ArrayList list = new ArrayList(attrList.size());
        for(int k=0; k<attrList.size(); k++){
            Object[] pair = (Object[])attrList.get(k);
            Name itemName = (Name)pair[0];
            CEGeneralAttributes attrs = (CEGeneralAttributes)pair[1];
            
            if(matchingAttributes == null || matchingAttributes.size() == 0 || attrs.match(matchingAttributes)) {

                Object obj = lookup(itemName);
                CEGeneralAttributes resultAttrs = attrs.clone(attributesToReturn);

                list.add(new SearchResult(itemName.toString(), obj, resultAttrs));
            }
        }

        return new CEGeneralEnumeration(list);
    }

    public NamingEnumeration search(String name, Attributes matchingAttributes, 
                                    String[] attributesToReturn) throws NamingException {
        return search(nameParser.parse(name), matchingAttributes, attributesToReturn);
    }

    public NamingEnumeration search(Name name, Attributes matchingAttributes) throws NamingException {
        return search(name.toString(), matchingAttributes, null);
    }

    public NamingEnumeration search(String name, Attributes matchingAttributes) throws NamingException {
        return search(name, matchingAttributes, null);
    }

    public NamingEnumeration search(Name name, String filter, SearchControls cons) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public NamingEnumeration search(String name, String filter, SearchControls cons) throws NamingException {
        return search(nameParser.parse(name), filter, cons);
    }

    public NamingEnumeration search(Name name, String filterExpr, Object[] filterArgs, 
                                    SearchControls cons) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public NamingEnumeration search(String name, String filterExpr, Object[] filterArgs, 
                                    SearchControls cons) throws NamingException {
        return search(nameParser.parse(name), filterExpr, filterArgs, cons);
    }



    public String toString() {
        return contextName.toString();
    }



    public void close() throws NamingException {}



    public void addNamingListener(Name target, int scope, NamingListener lsnr) throws NamingException {
        Name tmpName = ((Name) contextName.clone()).addAll(target);
        eventManager.register(tmpName, scope, lsnr);
    }



    public void addNamingListener(String target, int scope, NamingListener lsnr) throws NamingException {
        addNamingListener(nameParser.parse(target), scope, lsnr);
    }



    public void addNamingListener(Name target, String filter, Object[] filterArgs, 
                                  SearchControls ctls, NamingListener lsnr) throws NamingException {
        throw new OperationNotSupportedException();
    }



    public void addNamingListener(Name target, String filter, SearchControls ctls, 
                                  NamingListener lsnr) throws NamingException {
        throw new OperationNotSupportedException();
    }



    public void addNamingListener(String target, String filter, Object[] filterArgs, 
                                  SearchControls ctls, NamingListener lsnr) throws NamingException {
        throw new OperationNotSupportedException();
    }



    public void addNamingListener(String target, String filter, SearchControls ctls, 
                                  NamingListener lsrn) throws NamingException {
        throw new OperationNotSupportedException();
    }



    public void removeNamingListener(NamingListener lsnr) {
        eventManager.remove(contextName, lsnr);
    }



    public boolean targetMustExist() {
        return false;
    }



    protected void fireEvents(Name target, int type, Object newObj, Object oldObj, Object changeInfo) 
        throws NamingException {
        CEEventManager.Tuple[] tuple = eventManager.getListeners(target, type);

        for (int r = 0; r < tuple.length; r++) {
            Name sourceName = tuple[r].getSource();
            EventDirContext evnCtx = (EventDirContext) internalLookup(sourceName, true);

            Name itemName = ((Name) target.clone()).getSuffix(sourceName.size());
            Binding newBd = null;
            Binding oldBd = null;
            if(newObj != null)
                newBd = new Binding(itemName.toString(), newObj);
            if(oldObj != null)
                oldBd = new Binding(itemName.toString(), oldObj);

            NamingEvent evn = new NamingEvent(evnCtx, type, newBd, oldBd, changeInfo);

            logger.debug("Firing " + tuple[r].getSource().toString());
            switch(type) {
                case NamingEvent.OBJECT_ADDED:
                    ((NamespaceChangeListener) tuple[r].getListener()).objectAdded(evn);
                    break;
                case NamingEvent.OBJECT_REMOVED:
                    ((NamespaceChangeListener) tuple[r].getListener()).objectRemoved(evn);
                    break;
                case NamingEvent.OBJECT_RENAMED:
                    ((NamespaceChangeListener) tuple[r].getListener()).objectRenamed(evn);
                    break;
                case NamingEvent.OBJECT_CHANGED:
                    ((ObjectChangeListener) tuple[r].getListener()).objectChanged(evn);
            }
        }
    }

    public class CEGeneralEnumeration implements NamingEnumeration {
        Iterator iterator;

        public CEGeneralEnumeration(ArrayList lst) {
            iterator = lst.iterator();
        }

        public Object next() {
            return iterator.next();
        }

        public boolean hasMore() {
            return iterator.hasNext();
        }

        public void close() throws NamingException {}

        public boolean hasMoreElements() {
            return iterator.hasNext();
        }

        public Object nextElement() {
            return iterator.next();
        }
    }

}
