module org.nasdanika.docs {
	
	requires org.nasdanika.html.ecore;
	requires org.apache.commons.codec;
	requires org.eclipse.emf.codegen.ecore;
	requires org.eclipse.emf.ecore.xmi;
	requires net.sourceforge.plantuml;
	
	// Models	
	requires org.nasdanika.exec;
	requires org.nasdanika.ncore;
	
	requires org.nasdanika.html.model.html;
	requires org.nasdanika.html.model.bootstrap;
	requires org.nasdanika.html.model.app;
	
	requires org.nasdanika.architecture.c4;
	
	requires org.nasdanika.architecture.cloud.azure.core;
	requires org.nasdanika.architecture.cloud.azure.compute;
	requires org.nasdanika.architecture.cloud.azure.networking;
	requires org.nasdanika.architecture.cloud.azure.storage;
	
	requires org.nasdanika.architecture.containers.docker;
	requires org.nasdanika.architecture.containers.kubernetes;
	requires org.nasdanika.architecture.containers.helm;
	
}