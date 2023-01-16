package org.nasdanika.docs;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.apache.commons.codec.binary.Hex;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.GenPackage;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.nasdanika.common.Context;
import org.nasdanika.common.DiagramGenerator;
import org.nasdanika.common.MutableContext;
import org.nasdanika.common.NullProgressMonitor;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.emf.EObjectAdaptable;
import org.nasdanika.exec.ExecPackage;
import org.nasdanika.exec.content.ContentPackage;
import org.nasdanika.exec.resources.ResourcesPackage;
import org.nasdanika.html.ecore.EcoreActionSupplier;
import org.nasdanika.html.ecore.EcoreActionSupplierAdapterFactory;
import org.nasdanika.html.ecore.GenModelResourceSet;
import org.nasdanika.html.model.app.AppPackage;
import org.nasdanika.html.model.app.gen.ActionSiteGenerator;
import org.nasdanika.html.model.bootstrap.BootstrapPackage;
import org.nasdanika.html.model.html.HtmlPackage;
import org.nasdanika.ncore.NcorePackage;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;

public class DocGenerator {
	private static final File GENERATED_MODELS_BASE_DIR = new File("target/model-doc");
	private static final File ACTION_MODELS_DIR = new File(GENERATED_MODELS_BASE_DIR, "actions");
	private static final File RESOURCE_MODELS_DIR = new File(GENERATED_MODELS_BASE_DIR, "resources");
	
	private DiagramGenerator createDiagramGenerator(ProgressMonitor progressMonitor) {
//		FileSystemContainer output = new FileSystemContainer(new File("target\\diagram-cache"));
//		
//		BiFunction<String,InputStream,String> decoder = (path, state) -> DefaultConverter.INSTANCE.convert(state, String.class);
//		BiFunction<String,String,InputStream> encoder = (path, state) -> DefaultConverter.INSTANCE.convert(state, InputStream.class);
		
		DiagramGenerator plantUMLGenerator = new DiagramGenerator() {
			
			@Override
			public boolean isSupported(String dialect) {
				return DiagramGenerator.UML_DIALECT.equals(dialect)
						|| DiagramGenerator.GANTT_DIALECT.equals(dialect)
						|| DiagramGenerator.MINDMAP_DIALECT.equals(dialect)
						|| DiagramGenerator.SALT_DIALECT.equals(dialect)
						|| DiagramGenerator.WBS_DIALECT.equals(dialect);
			}
			
			@Override
			public String generateDiagram(String spec, String dialect) {
				try {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					StringBuilder sb = new StringBuilder("@start")
							.append(dialect)
							.append(System.lineSeparator())
							.append(spec)
							.append(System.lineSeparator())
							.append("@end")
							.append(dialect)
							.append(System.lineSeparator());
					
					SourceStringReader reader = new SourceStringReader(sb.toString());
					
					FileFormatOption fileFormatOption = new FileFormatOption(FileFormat.PNG);
					reader.outputImage(baos, 0, fileFormatOption);		
					String diagramCMap = reader.getCMapData(0, fileFormatOption);
					baos.close();
	
					StringBuilder ret = new StringBuilder("<img src=\"data:image/png;base64, ");
					ret
						.append(Base64.getEncoder().encodeToString(baos.toByteArray()))
						.append("\"");
					
					if (org.nasdanika.common.Util.isBlank(diagramCMap)) {
						ret.append("/>");
						return ret.toString();			
					}
					
					String openingTag = "<map id=\"plantuml_map\" name=\"plantuml_map\">";
					if (diagramCMap.startsWith(openingTag)) {
						String mapId = "plantuml_map_" + UUID.randomUUID().toString();
						ret			
						.append(" usemap=\"#")
						.append(mapId)
						.append("\"/>")
						.append(System.lineSeparator())
						.append("<map id=\"")
						.append(mapId)
						.append("\" name=\"")
						.append(mapId)
						.append("\">")
						.append(diagramCMap.substring(openingTag.length()));
						
					} else {				
						ret			
							.append(" usemap=\"#plant_uml_map\"/>")
							.append(System.lineSeparator())
							.append(diagramCMap);
						return ret.toString();
					}
											
					return ret.toString();			
				} catch (Exception e) {
					return "<div class=\"nsd-error\">Error during diagram rendering: " + e + "</div>";
				}
			}
		};
		return DiagramGenerator.INSTANCE.compose(plantUMLGenerator); //.cachingDiagramGenerator(output.stateAdapter().adapt(decoder, encoder), progressMonitor);
	}
	
	private boolean isSameURI(EPackage a, EPackage b) {
		return a !=  null && b != null && a.getNsURI().equals(b.getNsURI());
	}
		
	public void generateEcoreActionModel(Context context, ProgressMonitor progressMonitor) throws Exception {
		GenModelResourceSet ecoreModelsResourceSet = new GenModelResourceSet();
		
		Map<String,String> pathMap = new ConcurrentHashMap<>();
		
		Function<EPackage,String> getEPackagePath = ePackage -> {
			if (isSameURI(ePackage, NcorePackage.eINSTANCE)) {
				return "core/modules/ncore";
			}
			if (isSameURI(ePackage, ExecPackage.eINSTANCE)) {
				return "core/modules/exec/modules/model";
			}

			if (isSameURI(ePackage, AppPackage.eINSTANCE)) {
				return "html/modules/models/modules/app/modules/model";
			}
			if (isSameURI(ePackage, BootstrapPackage.eINSTANCE)) {
				return "html/modules/models/modules/bootstrap/modules/model";
			}
			if (isSameURI(ePackage, HtmlPackage.eINSTANCE)) {
				return "html/modules/models/modules/html/modules/model";
			}
			
			// TODO - NASDAF

			for (int i = 0; i < Integer.MAX_VALUE; ++i) {
				String path = i == 0 ? ePackage.getName() : ePackage.getName() + "_" + i;
				if (pathMap.containsKey(path)) {
					if (ePackage.getNsURI().equals(pathMap.get(path))) {
						return path;
					}
				} else {
					pathMap.put(path, ePackage.getNsURI());
					return path;
				}
			}
			
			// Encoding NS URI as HEX. Shall never reach this point.
			return Hex.encodeHexString(ePackage.getNsURI().getBytes(StandardCharsets.UTF_8));
		};
		
		ecoreModelsResourceSet.getAdapterFactories().add(new EcoreActionSupplierAdapterFactory(context, getEPackagePath, org.nasdanika.common.Util.createNasdanikaJavadocResolver(new File(".."), progressMonitor)) {
			
			@Override
			protected String getDiagramDialect() {
				return DiagramGenerator.UML_DIALECT;
			}
			
			/**
			 * Built-in resolution does not work in Java 11.
			 */
			@Override
			protected Object getEPackage(String nsURI) {
				switch (nsURI) {
				case ExecPackage.eNS_URI:
					return ExecPackage.eINSTANCE;
				case ContentPackage.eNS_URI:
					return ContentPackage.eINSTANCE;
				case ResourcesPackage.eNS_URI:
					return ResourcesPackage.eINSTANCE;
				case NcorePackage.eNS_URI:
					return NcorePackage.eINSTANCE;
				
				case HtmlPackage.eNS_URI:
					return HtmlPackage.eINSTANCE;
				case BootstrapPackage.eNS_URI:
					return BootstrapPackage.eINSTANCE;
				case AppPackage.eNS_URI:
					return AppPackage.eINSTANCE;
					
				// TODO - NASDAF	
				
				default:
					return super.getEPackage(nsURI);
				}
			}
			
		});
		
		// Physical location relative to the projects (git) root folder -> logical (workspace) name 
		Map<String,String> bundleMap = new LinkedHashMap<>();
		
		bundleMap.put("core/exec", "org.nasdanika.exec");
		bundleMap.put("core/ncore", "org.nasdanika.ncore");
				
		bundleMap.put("html/model/app", "org.nasdanika.html.model.app");
		bundleMap.put("html/model/bootstrap", "org.nasdanika.html.model.bootstrap");
		bundleMap.put("html/model/html", "org.nasdanika.html.model.html");

		// TODO - NASDAF
	
		File modelDir = new File("target/models").getAbsoluteFile();
		modelDir.mkdirs();
		
		File modelDocActionsDir = new File("target/model-doc/actions").getAbsoluteFile();
		org.nasdanika.common.Util.delete(modelDocActionsDir);
		modelDocActionsDir.mkdirs();
		
		Map<URI,File> modelToActionModelMap = new LinkedHashMap<>();
		
		File projectsRoot = new File("..");		
		for (Entry<String, String> be: bundleMap.entrySet()) {					
			File sourceDir = new File(projectsRoot, be.getKey());
			File targetDir = new File(modelDir, be.getValue());
			org.nasdanika.common.Util.copy(new File(sourceDir, "model"), new File(targetDir, "model"), true, (source, target) -> {
				if (target.getName().endsWith(".genmodel")) {
					modelToActionModelMap.put(URI.createFileURI(target.getAbsolutePath()), new File(modelDocActionsDir, target.getName() + ".xml"));
				}
			});			
			org.nasdanika.common.Util.copy(new File(sourceDir, "doc"), new File(targetDir, "doc"), true, null);
		}		
		
		// Loading resources to the resource set.
		for (URI uri: modelToActionModelMap.keySet()) {
			ecoreModelsResourceSet.getResource(uri, true);
		}		
		
		EcoreUtil.resolveAll(ecoreModelsResourceSet);
		
		ResourceSet actionModelsResourceSet = new ResourceSetImpl();
		actionModelsResourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
		
		// Generating
		for (URI uri: modelToActionModelMap.keySet()) {
			Resource ecoreModelResource = ecoreModelsResourceSet.getResource(uri, false);
			File output = modelToActionModelMap.get(ecoreModelResource.getURI());
			
			Resource actionModelResource = actionModelsResourceSet.createResource(URI.createFileURI(output.getAbsolutePath()));
			
			for (EObject contents: ecoreModelResource.getContents()) {
				if (contents instanceof GenModel) {
					for (GenPackage genPackage: ((GenModel) contents).getGenPackages()) {
						EPackage ecorePackage = genPackage.getEcorePackage();
						actionModelResource.getContents().add(EObjectAdaptable.adaptTo(ecorePackage, EcoreActionSupplier.class).execute(null, progressMonitor));
					}
				}
			}
	
			actionModelResource.save(null);
		}		
	}
	
	private void copyJavaDoc() throws Exception {
		org.nasdanika.common.Util.copy(new File("../core/target/site/apidocs"), new File("docs/core/apidocs"), false, null, null);		
		org.nasdanika.common.Util.copy(new File("../html/target/site/apidocs"), new File("docs/html/apidocs"), false, null, null);	
		// TODO - NASDAF
	}
	
	public void generate() throws Exception {
		org.nasdanika.common.Util.delete(ACTION_MODELS_DIR);
		org.nasdanika.common.Util.delete(RESOURCE_MODELS_DIR);
		
		ACTION_MODELS_DIR.mkdirs();
		RESOURCE_MODELS_DIR.mkdirs();

		ProgressMonitor progressMonitor = new NullProgressMonitor(); // PrintStreamProgressMonitor();		

		Function<String, String> nasdanikaResolver = org.nasdanika.common.Util.createNasdanikaJavadocResolver(new File(".."), progressMonitor);
		
		MutableContext context = Context.EMPTY_CONTEXT.fork();
		context.put("javadoc", org.nasdanika.common.Util.createJavadocPropertyComputer(nasdanikaResolver));
		
		DiagramGenerator diagramGenerator = createDiagramGenerator(progressMonitor);
		context.register(DiagramGenerator.class, diagramGenerator);//DiagramGenerator.createClient(new URL("http://localhost:8090/spring-exec/api/v1/exec/diagram/")));
		
		generateEcoreActionModel(context, progressMonitor);
		ActionSiteGenerator siteGenerator = new ActionSiteGenerator() {
			
			@Override
			protected MutableContext createContext(ProgressMonitor progressMonitor) {
				return context;
			}
			
			@Override
			protected boolean isSearch(File file, String path) {
				// TODO Auto-generated method stub
				return super.isSearch(file, path) 
					&& !"all-issues.html".equals(path)
					&& !"issues.html".equals(path)
					&& !"assignments.html".equals(path)
					&& !path.endsWith("/all-issues.html")
					&& !path.endsWith("/issues.html")
					&& !path.endsWith("/assignments.html")
					&& !path.endsWith("-load-specification.html")
					&& !path.endsWith("-all-operations.html")
					&& !path.endsWith("-all-attributes.html")
					&& !path.endsWith("-all-references.html")
					&& !path.endsWith("-all-supertypes.html");				
			}
			
			@Override
			protected boolean isDeleteOutputPath(String path) {
				return !"CNAME".equals(path) && !"favicon.ico".equals(path) && !path.startsWith("images/");
			}
			
			@Override
			protected ProgressMonitor createProgressMonitor() {
				return progressMonitor.split("Site generation", 1);
			}
			
		};
				
		String rootActionResource = "model/actions.yml";
		URI rootActionURI = URI.createFileURI(new File(rootActionResource).getAbsolutePath());
		
		String pageTemplateResource = "model/page-template.yml";
		URI pageTemplateURI = URI.createFileURI(new File(pageTemplateResource).getAbsolutePath());
		
		Map<String, Collection<String>> errors = siteGenerator.generate(
				rootActionURI, 
				pageTemplateURI, 
				"https://docs.nasdanika.org", 
				new File("docs"), 
				new File("target/action-site"), 
				false);
				
		for (Entry<String, Collection<String>> ee: errors.entrySet()) {
			System.err.println(ee.getKey());
			for (String error: ee.getValue()) {
				System.err.println("\t" + error);
			}
		}
		
		copyJavaDoc();		
	}

}
