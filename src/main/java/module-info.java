module org.nasdanika.docs {
	
	requires org.nasdanika.html.ecore;
	requires org.apache.commons.codec;
	requires org.eclipse.emf.codegen.ecore;
	requires org.eclipse.emf.ecore.xmi;
	requires plantuml;
	
	// Models	
	requires org.nasdanika.exec;
	requires org.nasdanika.ncore;
	requires org.nasdanika.html.model.html;
	requires org.nasdanika.html.model.bootstrap;
	requires org.nasdanika.html.model.app;
	
}