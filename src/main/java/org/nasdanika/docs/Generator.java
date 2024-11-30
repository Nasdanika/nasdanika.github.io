package org.nasdanika.docs;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.emf.common.util.URI;
import org.nasdanika.common.ExecutionException;
import org.nasdanika.models.app.gen.AppSiteGenerator;

public class Generator {
	
	public static void main(String[] args) throws Exception {
		AppSiteGenerator actionSiteGenerator = new AppSiteGenerator() {
			
			@Override
			protected boolean isDeleteOutputPath(String path) {
				return !"CNAME".equals(path) && !"favicon.ico".equals(path) && !path.startsWith("images/") && !path.startsWith("demos/");
			}			
			
		};
		
		File nasdanikaDiagramFile = new File("model/nasdanika.drawio").getCanonicalFile();
		String pageTemplateResource = "model/page-template.yml";
		URI pageTemplateURI = URI.createFileURI(new File(pageTemplateResource).getAbsolutePath());
		
		Map<String, Collection<String>> errors = actionSiteGenerator.generate(
				URI.createFileURI(nasdanikaDiagramFile.getAbsolutePath()).appendFragment("/"), 
				pageTemplateURI, 
				"https://docs.nasdanika.org", 
				new File("docs"),  
				new File("target/doc-site-work-dir"), 
				false);
				
		int errorCount = 0;
		for (Entry<String, Collection<String>> ee: errors.entrySet()) {
			System.err.println(ee.getKey());
			for (String error: ee.getValue()) {
				System.err.println("\t" + error);
				++errorCount;
			}
		}
		
		System.out.println("There are " + errorCount + " site errors");
		
		if (errors.size() != 11) {
			throw new ExecutionException("There are problems with pages: " + errorCount);
		}				
		
	}

	
}
