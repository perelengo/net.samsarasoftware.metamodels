package net.samsarasoftware.metamodels;

/*-
 * #%L
 * net.samsarasoftware.metamodels
 * %%
 * Copyright (C) 2014 - 2017 Pere Joseph Rodriguez
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
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.Collection;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.uml2.uml.Profile;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.UMLPlugin;
import org.eclipse.uml2.uml.resource.UMLResource;


public class PluginDescriptorGenerator {

	protected StringBuffer buffer=new StringBuffer();
	
	public static void main(String[] args) {
		PluginDescriptorGenerator s=null;
		try {
		
			s = new PluginDescriptorGenerator();
			s.parseParams(args);
			s.configure();
			s.run();
		} catch (Exception e) {
			e.printStackTrace(); 
		}finally{
		}	
	}

	private void run() throws Exception {
		File profilesDir=new File(FILE_ORIGEN);
		if(!profilesDir.exists()) throw new Exception("No existe el directorio "+FILE_ORIGEN);
		File destDir=new File(FILE_DESTINO);
		if(!destDir.exists()) destDir.getParentFile().mkdirs();
		
		
		buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		buffer.append("<?eclipse version=\"3.4\"?>\n");
		buffer.append("<plugin>\n");
		
		ResourceSet resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(UMLResource.FILE_EXTENSION, UMLResource.Factory.INSTANCE);
		EPackage.Registry.INSTANCE.put(org.eclipse.uml2.uml.UMLPackage.eINSTANCE.getNsURI(), org.eclipse.uml2.uml.UMLPackage.eINSTANCE);
        registeProfilesDir(resourceSet,profilesDir);
        
        buffer.append("	<extension point=\"org.eclipse.ocl.pivot.complete_ocl_registry\">\n");
		buffer.append("		<document resource=\"constraints/security_constraints.ocl\">\n");
		buffer.append("			<for uri=\"http://www.eclipse.org/uml2/5.0.0/UML\"/>\n");
		buffer.append("		</document>\n");
		buffer.append("	</extension>\n");
		
        buffer.append("	<extension point=\"org.eclipse.ocl.pivot.complete_ocl_registry\">\n");
		buffer.append("		<document resource=\"constraints/service_constraints.ocl\">\n");
		buffer.append("			<for uri=\"http://www.eclipse.org/uml2/5.0.0/UML\"/>\n");
		buffer.append("		</document>\n");
		buffer.append("	</extension>\n");
		
        buffer.append("	<extension point=\"org.eclipse.ocl.pivot.complete_ocl_registry\">\n");
		buffer.append("		<document resource=\"constraints/sql_constraints.ocl\">\n");
		buffer.append("			<for uri=\"http://www.eclipse.org/uml2/5.0.0/UML\"/>\n");
		buffer.append("		</document>\n");
		buffer.append("	</extension>\n");
		
		buffer.append("</plugin>");

		File out=new File(destDir, "plugin.xml");
		FileOutputStream fout=new FileOutputStream(out);
		fout.write(buffer.toString().getBytes("UTF-8"));
		fout.close();
		
	}

	private void registeProfilesDir(ResourceSet resourceSet, File libFolder) {

        File[] libs=libFolder.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.getAbsolutePath().endsWith(".profile.uml") || pathname.isDirectory();
			}
		});
        
        if(libs!=null){
            for(int i=0;i<libs.length;i++){
                if(libs[i].isDirectory()){
                	registeProfilesDir(resourceSet,libs[i]);
                }else{
                	registerProfile(resourceSet, URI.createURI("file:///"+libs[i].getAbsolutePath()));
                }
            }
        }	
	}
    private void registerProfile(ResourceSet resourceSet, URI uri) {
        Resource profileResource = resourceSet.getResource(uri, true);
        
        Profile profile=((Profile)EcoreUtil.getObjectByType(profileResource.getContents(),UMLPackage.Literals.PROFILE));
        if(profile.getURI()!=null){
        	EPackage.Registry.INSTANCE.put(profile.getURI().toString(),profile.getDefinition());
        	EcorePlugin.INSTANCE.log("Registered "+profile.getURI().toString()+" as "+EcoreUtil.getURI(profile));
        	Resource epackageResource = resourceSet.getResource(uri,true);
			EPackage rootPackage=(EPackage) profile.getEAnnotation("http://www.eclipse.org/uml2/2.0.0/UML").eContents().get(0);
    		
    		String profileUri=EcoreUtil.getURI(profile).toString();
    		String relativeUri=profileUri.substring(profileUri.indexOf("net.samsarasoftware.metamodels")+("net.samsarasoftware.metamodels".length())+1).replaceAll("\\\\", "/");

    		String epackageUri=EcoreUtil.getURI(rootPackage).toString();
    		String relativeUri2=epackageUri.substring(epackageUri.indexOf("net.samsarasoftware.metamodels")+("net.samsarasoftware.metamodels".length())+1).replaceAll("\\\\", "/");
    		
    		String pluginUri="platform:/plugin/net.samsarasoftware.metamodels/"+relativeUri;
    		String ecoreUri="platform:/plugin/net.samsarasoftware.metamodels/"+relativeUri2;
    		String genmodelRelative=relativeUri.replace("profile.uml", "genmodel");
    		genmodelRelative=genmodelRelative.substring(0,genmodelRelative.indexOf("#"));
    		//ecoreUri=ecoreUri.substring(0,ecoreUri.indexOf("#"));
    		
    		buffer.append("   <extension point=\"org.eclipse.emf.ecore.generated_package\">\n");
    		buffer.append("   	<package \n");
    		buffer.append("   		uri = \""+(profile.getURI().toString().replace(".profile", ".ecore"))+"\"\n");
    		buffer.append("   		class = \""+profile.getName()+"."+profile.getName()+"Package\"\n");
    		buffer.append("   		genModel=\""+genmodelRelative+"\"");
    		buffer.append("   	/>\n");
    		buffer.append("   </extension>\n");

//    		buffer.append("   <extension point=\"org.eclipse.uml2.uml.generated_package\">\n");
//    		buffer.append("   	<profile \n");
//    		buffer.append("   		uri = \""+(profile.getURI().toString())+"\"\n");
//    		buffer.append("   		location = \""+pluginUri+"\" />\n");
//    		buffer.append("   </extension>\n");
    	        
//        	buffer.append("   <extension point=\"org.eclipse.uml2.uml.dynamic_package\">\n");
//    		buffer.append("		<profile\n");
//    		buffer.append("			uri=\"");
//    		buffer.append(profile.getURI().toString());
//    		buffer.append("\"\n");
//    		buffer.append("			location=\"");
//    		buffer.append(pluginUri);
//    		buffer.append("\">\n");
//    		buffer.append("      </profile>\n");
//    		buffer.append("   </extension>\n");
    		
    		buffer.append("   <extension point=\"org.eclipse.papyrus.uml.extensionpoints.UMLProfile\">\n");
    		buffer.append("		<profile\n");
    		buffer.append("			name=\"");
    		buffer.append(profile.getName());
    		buffer.append("\"\n");
    		buffer.append("			uri=\"");
    		buffer.append(profile.getURI().toString());
    		buffer.append("\"\n");
    		buffer.append("			path=\"");
    		buffer.append(ecoreUri);
    		buffer.append("\">\n");
    		buffer.append("      </profile>\n");
    		buffer.append("   </extension>\n");
        }
	}
	
	private void configure() {
		
	}

	private String FILE_ORIGEN;
	private String FILE_DESTINO;
	

	private void parseParams(String[] args) throws Exception {
			if(args.length<4) printUsage();

		   for (int i=0;i<args.length;i++) {
				if("-profilesDir".equals(args[i])){
					FILE_ORIGEN=args[++i];
				}else if("-targetDir".equals(args[i])){
					FILE_DESTINO=args[++i];
				}else{
					printUsage();
				}
			}
		   
		   if(FILE_DESTINO==null) printUsage();
		   if(FILE_ORIGEN==null) printUsage();
		}
	   
	private void printUsage() throws Exception {
		throw new Exception("Errores en los argumentos. Uso:\n java -jar <nombrejar>.jar \n "
				+ "[-profilesDir <directorio de profilesa >]\n "
				+ "[-targetDir <directorio de salida para el plugin.xml>]"	
				+ "");
	}
}
