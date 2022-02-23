package net.samsarasoftware.metamodels;

/*-
 * #%L
 * net.samsarasoftware.metamodels
 * %%
 * Copyright (C) 2014 - 2020 Pere Joseph Rodriguez
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 * #L%
 */

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.Path;
import org.eclipse.emf.codegen.ecore.generator.Generator;
import org.eclipse.emf.codegen.ecore.generator.GeneratorAdapterFactory;
import org.eclipse.emf.codegen.ecore.genmodel.GenJDKLevel;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.GenModelFactory;
import org.eclipse.emf.codegen.ecore.genmodel.GenModelPackage;
import org.eclipse.emf.codegen.ecore.genmodel.GenPackage;
import org.eclipse.emf.codegen.ecore.genmodel.generator.GenBaseGeneratorAdapter;
import org.eclipse.emf.codegen.ecore.genmodel.generator.GenModelGeneratorAdapterFactory;
import org.eclipse.emf.codegen.ecore.genmodel.impl.GenModelFactoryImpl;
import org.eclipse.emf.common.EMFPlugin;
import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.emf.ecore.plugin.EcorePlugin.ExtensionProcessor;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.Resource.Factory.Registry;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceFactoryImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.eclipse.uml2.uml.Profile;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.UMLPlugin;
import org.eclipse.uml2.uml.internal.resource.UMLResourceFactoryImpl;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.loader.AntClassLoader5;

public class GenModelGenerator {
	List<URI> unresolvedProfiles=new ArrayList<URI>();

	private String FILE_ORIGEN;
	List<GenPackage> generatedGenPackages=new ArrayList<GenPackage>();

	
	
	 public GenModel createGenModel(final EPackage rootPackage, final String ecoreLocation) {

		        GenModel genModel = GenModelFactory.eINSTANCE.createGenModel();
		        genModel.setComplianceLevel(GenJDKLevel.JDK70_LITERAL);
		        genModel.setModelDirectory("/net.samsarasoftware.metamodels/src-gen");
		        genModel.getForeignModel().add(new Path(ecoreLocation).lastSegment());
		        rootPackage.setNsURI(rootPackage.getNsURI().replace(".profile", ".ecore"));
		        genModel.setModelName(rootPackage.getName());
		        genModel.setRootExtendsClass("org.eclipse.emf.ecore.impl.EObjectImpl");
		        genModel.setRootExtendsInterface("org.eclipse.emf.ecore.EObject");
		        genModel.initialize(Collections.singleton(rootPackage));
		        
		        GenPackage genPackage = (GenPackage)genModel.getGenPackages().get(0);
		        genPackage.setPrefix(rootPackage.getName());

		        return genModel;
		    }
	 
	 public void exportGenModel(ResourceSet rs, EObject genModel, String genModelLocation){
		try {
	            URI genModelURI = URI.createPlatformPluginURI(genModelLocation+".genmodel",true);
	            
	            final XMIResourceImpl genModelResource = (XMIResourceImpl) rs.createResource(genModelURI);
	            
	            //Resource ecoreRes = rs.createResource(URI.createFileURI(genModelLocation+".ecore"));
	            //ecoreRes.getContents().add(genModel);
	            //EcoreUtil.getURI(genModel)
	            genModelResource.getDefaultSaveOptions().put(XMLResource.OPTION_ENCODING,
	                "UTF-8");
	            genModelResource.getContents().add(genModel);
	            genModelResource.save(Collections.EMPTY_MAP);
	        } catch (IOException e) {
	            String msg = null;
	            final String genModelLocationCleaned = genModelLocation;
	            if (e instanceof FileNotFoundException) {
	                msg = "Unable to open output file " + genModelLocationCleaned;
	            } else {
	                msg = "Unexpected IO Exception writing " + genModelLocationCleaned;
	            }
	            e.printStackTrace();
	            throw new RuntimeException(msg, e);
	        }
	 }
	 
	 public static void main(String[] args) {
		
		GenModelGenerator genModelGenerator=new GenModelGenerator();
		try {
			genModelGenerator.parseParams(args);
			
			genModelGenerator.run();
		} catch (Exception e) {
			e.printStackTrace();
		}

		
	}
	 
	private void run() throws Exception {
		
		File profilesDir=new File(FILE_ORIGEN);
		if(!profilesDir.exists()) throw new Exception("No existe el directorio "+FILE_ORIGEN);
		registeProfilesDir( profilesDir);
		List<URI> unresolvedProfiles2;
		do{
			
			unresolvedProfiles2=(List<URI>) ((ArrayList)unresolvedProfiles).clone();
			Collections.copy(unresolvedProfiles2, unresolvedProfiles);
			unresolvedProfiles.clear();
			for (URI uri2 : unresolvedProfiles2) {
				System.out.println("----- Unresolved -------");
	        	System.out.println(uri2.toString());
	        	System.out.println("------------------------");
			}
	        for (URI uri2 : unresolvedProfiles2) {
	        	try{
	        		registerProfile(unresolvedProfiles2, uri2);
	        	}catch (Exception e) {
    	        	e.printStackTrace();
    				unresolvedProfiles.add(uri2);
    			}
			}
        }while(unresolvedProfiles.size()>0 && !(unresolvedProfiles.equals(unresolvedProfiles2)));
	}
	
	private void registeProfilesDir(File libFolder) {

        File[] libs=libFolder.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.getAbsolutePath().endsWith(".profile.uml") || pathname.isDirectory();
			}
		});
        
        if(libs!=null){
            for(int i=0;i<libs.length;i++){
                if(libs[i].isDirectory()){
                	registeProfilesDir(libs[i]);
                }else{
                	try{
                		registerProfile( new ArrayList<URI>(), URI.createPlatformPluginURI("net.samsarasoftware.metamodels/profiles/"+libs[i].getName(), true));
        			}catch (Exception e) {
        	        	e.printStackTrace();
        				unresolvedProfiles.add(URI.createPlatformPluginURI("net.samsarasoftware.metamodels/profiles/"+libs[i].getName(), true));
        			}
                }
            }
        }	
	}
    private void registerProfile( List<URI> unresolvedProfiles, URI uri) {
    	
    	ResourceSet resourceSet=this.configure();
        System.out.println("-----------------------------------------");
        System.out.println("- Building genmodel for "+uri.lastSegment());
        System.out.println("-----------------------------------------");
        
        	runGenModel(resourceSet,uri);
        
        
    }
	private void runGenModel(ResourceSet resourceSet, URI uri){
		
        Resource umlGenModelResource = null;
    	umlGenModelResource=resourceSet.getResource(
    			URI.createPlatformPluginURI("org.eclipse.uml2.uml/model/UML.genmodel",false)
            	, true);
    	EList<GenPackage> umlGenPackages = ((GenModel)umlGenModelResource.getContents().get(0)).getGenPackages();

        Resource umlTypesGenModelResource = null;
        umlTypesGenModelResource=resourceSet.getResource(
    			URI.createPlatformPluginURI("org.eclipse.uml2.types/model/Types.genmodel",false)
            	, true);
    	EList<GenPackage> umlTypesGenPackages = ((GenModel)umlTypesGenModelResource.getContents().get(0)).getGenPackages();

    	
        Resource umlStandardProfileGenModelResource = null;
        umlStandardProfileGenModelResource=resourceSet.getResource(
    			URI.createPlatformPluginURI("org.eclipse.uml2.uml.profile.standard/model/Standard.genmodel",false)
            	, true);
    	EList<GenPackage> umlStandardProfileGenPackage = ((GenModel)umlTypesGenModelResource.getContents().get(0)).getGenPackages();
    	
    	
		//Resource epackageResource = resourceSet.getResource(URI.createFileURI("C:/Users/perel/workspace/base_project_test/src/main/bak/base_project.uml"),true);
		Resource epackageResource = resourceSet.getResource(uri,true);
		Collection<Profile> profiles=EcoreUtil.getObjectsByType(epackageResource.getContents(),UMLPackage.Literals.PROFILE);
		for (Profile profile : profiles) {
				EPackage rootPackage=(EPackage) profile.getEAnnotation("http://www.eclipse.org/uml2/2.0.0/UML").eContents().get(0);
				GenModel genModel = this.createGenModel(rootPackage, EcoreUtil.getURI(rootPackage).toString() );
				try{
					//EcoreUtil.getURI(rootPackage);
					genModel.getUsedGenPackages().addAll(umlGenPackages);
					genModel.getUsedGenPackages().addAll(umlTypesGenPackages);
					genModel.getUsedGenPackages().addAll(umlStandardProfileGenPackage);
					genModel.getUsedGenPackages().addAll(generatedGenPackages);
					
					genModel.setCanGenerate(true);
					// Globally register the default generator adapter factory for GenModel
					   // elements (only needed in stand-alone).
					   // 
					   GeneratorAdapterFactory.Descriptor.Registry.INSTANCE.addDescriptor
					     (GenModelPackage.eNS_URI, GenModelGeneratorAdapterFactory.DESCRIPTOR);
					 
					   // Create the generator and set the model-level input object.
					   // 
					   Generator generator = new Generator();
					   generator.setInput(genModel);
					 
					   // Generator model code.
					   genModel.setNonNLSMarkers(true);
					   genModel.setOSGiCompatible(true);
					   //EcoreUtil.getURI(genModel.getGenPackages().get(0).getGenClasses().get(0))
					   
					   this.exportGenModel(resourceSet,genModel, "net.samsarasoftware.metamodels/profiles/"+rootPackage.getName());
					   generator.generate
					     (genModel, GenBaseGeneratorAdapter.MODEL_PROJECT_TYPE,
					      new BasicMonitor.Printing(System.out));
				       
					   rootPackage.setNsURI(rootPackage.getNsURI().replace(".ecore",".profile" ));

					   generatedGenPackages.add(genModel.getGenPackages().get(0));

				}catch(Throwable t){
					generatedGenPackages.remove(genModel.getGenPackages().get(0));
					throw t;
				}

		}		
	}

	public void initialize(ResourceSet resourceSet){
        Map uriMap = resourceSet.getURIConverter().getURIMap();

        //Esto sirve para plugins con plugin.xml
        
        if(!EMFPlugin.IS_ECLIPSE_RUNNING){
        	String[] cpFiles=new String[0];
        	
        	ClassLoader classLoader=(ClassLoader) (Thread.currentThread().getContextClassLoader());
            ExtensionProcessor.process(classLoader);
            	
            String classpath = null;
            try
            {
            	
            	Object[] ucp=null;
            	if(classLoader instanceof URLClassLoader){
            			ucp=((URLClassLoader)classLoader).getURLs();
            			cpFiles=new String[ucp.length];

                    	for (int i=0;i<ucp.length;i++) {
        					cpFiles[i]=ucp[i].toString();
        				}
            	}else if(classLoader.getClass().getName().contains("AntClassLoader")){
            		String ucpString = ((String)classLoader.getClass().getMethod("getClasspath").invoke(classLoader));
            		ucp = (ucpString.indexOf(";")!=-1)?ucpString.split(";"):ucpString.split(":"); //; for windows : for linux
            		
        			cpFiles=new String[ucp.length];

                	for (int i=0;i<ucp.length;i++) {
    					cpFiles[i]=ucp[i].toString();
    				}
            		
            	}
    	        
            }
            catch (Throwable throwable)
            {
              // Failing thet, get it from the system properties.
              throwable.printStackTrace();
              classpath = System.getProperty("java.class.path");
              cpFiles=(classpath.indexOf(";")!=-1)?classpath.split(";"):classpath.split(":"); //; for windows : for linux
            }
        	
           Pattern bundleSymbolNamePattern = Pattern.compile("^\\s*Bundle-SymbolicName\\s*:\\s*([^\\s;]*)\\s*([^\\s;]*)(;.*)?$", Pattern.MULTILINE);
	        
	        for (String filePath : cpFiles) {
	        	//System.out.println(filePath);
	        	InputStream inputStream = null;
	            try
	            {
		        	
		        	byte bytes[]=new byte[1024];
		        	String pluginID=null;
		        	URI baseUri=null;
		        	
		        	String basePath = filePath;
		        	String baseURL=null;
		        	
		            if(basePath.endsWith(".jar")){
		             	try{
		             		try{
		             			baseURL="file:/"+basePath+"!/META-INF/MANIFEST.MF";
		             			inputStream = new URL("jar:file:/"+basePath+"!/META-INF/MANIFEST.MF").openStream();
		             		}catch(Exception e1){
		             			baseURL=basePath+"!/META-INF/MANIFEST.MF";
		             			inputStream = new URL("jar:"+baseURL).openStream();
		             		}
		             		
		             		
		             	}catch(Exception e){}
		            }else{
	             		try{
	             			baseURL="file:///"+basePath.replace("file:/","")+"/META-INF/MANIFEST.MF";
	             			inputStream = new URL(baseURL).openStream();
	             		}catch(Exception e1){
	             			try{
	             				baseURL=basePath+"/META-INF/MANIFEST.MF";
	             				inputStream = new URL(baseURL).openStream();
		             		}catch(Exception e2){ //check for local compiled plugins
			             		try{
			             			baseURL=URI.createURI(new URL("file:///"+basePath).toString()).trimSegments(1).toString()+"/META-INF/MANIFEST.MF";
			             			inputStream = new URL(baseURL).openStream();
			             		}catch(Exception e3){
			             			try{
			             				baseURL=URI.createURI(new URL(basePath).toString()).trimSegments(1).toString()+"/META-INF/MANIFEST.MF";
			             				inputStream = new URL(baseURL).openStream();
				             		}catch(Exception e4){ //Check for local maven compiled plugins
					             		try{
					             			baseURL=URI.createURI(new URL("file:///"+basePath).toString()).trimSegments(2).toString()+"/META-INF/MANIFEST.MF";
					             			inputStream = new URL(baseURL).openStream();
					             		}catch(Exception e5){
					             			try{
					             				baseURL=URI.createURI(new URL(basePath).toString()).trimSegments(2).toString()+"/META-INF/MANIFEST.MF";
					             				inputStream = new URL(baseURL).openStream();
						             		}catch(Exception e6){
							             		try{
							             			baseURL=URI.createURI(new URL("file:///"+basePath).toString()).trimSegments(3).toString()+"/META-INF/MANIFEST.MF";
							             			inputStream = new URL(baseURL).openStream();
							             		}catch(Exception e7){
							             			try{
							             				baseURL=URI.createURI(new URL(basePath).toString()).trimSegments(3).toString()+"/META-INF/MANIFEST.MF";
							             				inputStream = new URL(baseURL).openStream();
								             		}catch(Exception e8){
								             			
								             		}
							             		}
						             		}
					             		}
				             		}
			             		}		             			
		             		}
	             		}
		            }
		            if (inputStream!=null)
		            {
	              
	
		                int available = inputStream.available();
		                if (bytes.length < available)
		                {
		                  bytes = new byte [available];
		                }
		                inputStream.read(bytes);
		                String contents = new String(bytes, "UTF-8");
		                Matcher matcher = bundleSymbolNamePattern.matcher(contents);
		                if (matcher.find())
		                {
		                	pluginID = matcher.group(1)+matcher.group(2);
		                	URI platformPluginURI = URI.createPlatformPluginURI(pluginID +"/", false);
		                	URI platformResourceURI = URI.createPlatformResourceURI(pluginID +"/",  true);
		                	if(!resourceSet.getURIConverter().exists(platformPluginURI, null)){
		                	 	  
			                  	  
			                  	  if(baseURL.indexOf(pluginID)!=-1){
				                	  Object resolvedURL=null;
				                	  
				                	  if(!basePath.endsWith(".jar")){
				                		  
				                		  resolvedURL=baseURL.substring(0,
				                				  baseURL.indexOf(pluginID)+pluginID.length()+1);
				                		  if(EcorePlugin.getPlatformResourceMap().get(pluginID)==null)
				                			  EcorePlugin.getPlatformResourceMap().put(pluginID, 
							                		URI.createURI((String) resolvedURL)
					                		  );
				                		  if(pluginID.equals("net.samsarasoftware.metamodels")){
							                  uriMap.put(platformPluginURI, URI.createFileURI((String)basePath.replace("file:/", "")+"/"));//.replace("target/classes", "")));
							                  System.out.println(platformPluginURI.toString()+" -> "+URI.createFileURI((String)basePath.replace("file:/", "")+"/"));
							                  uriMap.put(platformResourceURI, URI.createFileURI((String)basePath.replace("file:/", "")+"/"));//.replace("target/classes", "")));
							                  System.out.println(platformResourceURI.toString()+" -> "+URI.createFileURI((String)basePath.replace("file:/", "")+"/"));//.replace("target/classes", "")));
				                			  
				                		  }else{
							                  //aqu? puede ser que tengamos una referencia plugin o una referencia resource->classes
					                		  //hacia un solo proyecto. Elproblema es que para la referencia resource tenemos el directorio target/classes
					                		  //pero para la referencia plugin no.
					                		  //asi que cogemos del path
							                  uriMap.put(platformPluginURI, URI.createURI((String)basePath));
							                  System.out.println(platformPluginURI.toString()+" -> "+URI.createURI((String)basePath).toString());
							                  uriMap.put(platformResourceURI, URI.createURI((String) resolvedURL));
							                  System.out.println(platformResourceURI.toString()+" -> "+URI.createURI((String) resolvedURL).toString());
				                		  }
						                  File resolvedDir=new File(basePath.toString().replace("file:/",""));
					                	  //registeProfilesDir(resourceSet, resolvedDir,platformPluginURI);
				                	  }else{
				                		  resolvedURL="jar:"+baseURL.substring(0,
				                				  baseURL.indexOf("!")+2);
				                		  if(EcorePlugin.getPlatformResourceMap().get(pluginID)==null)
				                			  EcorePlugin.getPlatformResourceMap().put(pluginID, 
							                		URI.createURI((String) resolvedURL)
					                		  );

						                  
				                		  uriMap.put(platformPluginURI, URI.createURI((String) resolvedURL));
				                		  System.out.println(platformPluginURI.toString()+" -> "+URI.createURI(basePath).toString());
						                  uriMap.put(platformResourceURI.trimSegments(1).appendSegment("target").appendSegment("classes").appendSegment(""), URI.createURI((String) resolvedURL));
						                  System.out.println(platformResourceURI.trimSegments(1).appendSegment("target").appendSegment("classes").appendSegment("").toString()+" -> "+URI.createURI((String) resolvedURL).toString());
						                  
				                		  //registerJarProfiles(resourceSet, (String) baseURL.substring(0,baseURL.indexOf("!")).replace("file:/", ""),platformPluginURI);
				                	  }

				                  		
				                  		
		                	  		}
		                	  
		                	}
		                }
		            }
	            }catch (Exception exception)
	              {
	            	  exception.printStackTrace();
	              }
	              finally
	              {
	                if (inputStream != null)
	                {
	                  try
	                  {
	                    inputStream.close();
	                  }
	                  catch (IOException exception)
	                  {
	                	  exception.printStackTrace();
	                  }
	                  
	                }
	              }
	            }
        }
    	
    	EPackage.Registry.INSTANCE.put(org.eclipse.uml2.types.TypesPackage.eINSTANCE.getNsURI(), org.eclipse.uml2.types.TypesPackage.eINSTANCE);
    	EPackage.Registry.INSTANCE.put(org.eclipse.uml2.uml.UMLPackage.eINSTANCE.getNsURI(), org.eclipse.uml2.uml.UMLPackage.eINSTANCE);
    	EPackage.Registry.INSTANCE.put(org.eclipse.uml2.uml.profile.standard.StandardPackage.eINSTANCE.getNsURI(), org.eclipse.uml2.uml.profile.standard.StandardPackage.eINSTANCE);
    	EPackage.Registry.INSTANCE.put(EcorePackage.eINSTANCE.getNsURI(), EcorePackage.eINSTANCE);
    	EPackage.Registry.INSTANCE.put(GenModelPackage.eINSTANCE.getNsURI(),GenModelPackage.eINSTANCE);
    	
    	
        //Register basic libraries uri handlers
        String resourcesPlugin = "org/eclipse/uml2/uml/resources/ResourcesPlugin.class";
        URL url = this.getClass().getClassLoader().getResource( resourcesPlugin );
        if(url!=null){
        	String baseUrl = url.toString();    
            baseUrl = baseUrl.substring( 0, baseUrl.length() - resourcesPlugin.length() );
            URI baseUri=URI.createURI(baseUrl);
            uriMap.put(URI.createURI( UMLResource.LIBRARIES_PATHMAP ), baseUri.appendSegment( "libraries" ).appendSegment( "" ));
            uriMap.put(URI.createURI( UMLResource.METAMODELS_PATHMAP ), baseUri.appendSegment( "metamodels" ).appendSegment( "" ));
            uriMap.put(URI.createURI( UMLResource.PROFILES_PATHMAP ), baseUri.appendSegment( "profiles" ).appendSegment( "" ));
//            try {
//    			registerJarProfiles(resourceSet,baseUrl.replace("jar:file:/","").replace("!/", ""), URI.createPlatformPluginURI("org.eclipse.uml2.uml.resources",false));
//    		} catch (IOException e) {
//    			e.printStackTrace();
//    		}
        }
  
        UMLPlugin.getEPackageNsURIToProfileLocationMap().put(org.eclipse.uml2.uml.profile.standard.StandardPackage.eINSTANCE.getNsURI()
        		, URI.createURI("pathmap://UML_PROFILES/Standard.profile.uml#_0"));
        

	}

	private ResourceSet configure() {
		Registry reg = Resource.Factory.Registry.INSTANCE;
		ResourceFactoryImpl resFacImpl=new ResourceFactoryImpl();
		
		ResourceSet resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(UMLResource.FILE_EXTENSION, UMLResource.Factory.INSTANCE);        
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("ecore",  new XMIResourceFactoryImpl());
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("genmodel",  new XMIResourceFactoryImpl());
		
		this.initialize(resourceSet);

        return resourceSet;
	}

	

	private void parseParams(String[] args) throws Exception {
			if(args.length<2) printUsage();

		   for (int i=0;i<args.length;i++) {
				if("-profilesDir".equals(args[i])){
					FILE_ORIGEN=args[++i];
				}else{
					printUsage();
				}
			}
		   
		   if(FILE_ORIGEN==null) printUsage();
	}
	   
	private void printUsage() throws Exception {
		throw new Exception("Errores en los argumentos. Uso:\n java -jar <nombrejar>.jar \n "
				+ "[-profilesDir <directorio de profiles >]\n "
				+ "");
	}
}
