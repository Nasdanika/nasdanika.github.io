package org.nasdanika.docs;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.codec.binary.Hex;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.GenPackage;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.nasdanika.common.ConsumerFactory;
import org.nasdanika.common.Context;
import org.nasdanika.common.DefaultConverter;
import org.nasdanika.common.Diagnostic;
import org.nasdanika.common.DiagnosticException;
import org.nasdanika.common.DiagramGenerator;
import org.nasdanika.common.ExecutionException;
import org.nasdanika.common.MutableContext;
import org.nasdanika.common.NasdanikaException;
import org.nasdanika.common.NullProgressMonitor;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.common.Status;
import org.nasdanika.common.Supplier;
import org.nasdanika.common.SupplierFactory;
import org.nasdanika.diagram.DiagramPackage;
import org.nasdanika.emf.EObjectAdaptable;
import org.nasdanika.emf.EmfUtil;
import org.nasdanika.emf.persistence.EObjectLoader;
import org.nasdanika.emf.persistence.FeatureCacheAdapter;
import org.nasdanika.emf.persistence.YamlResourceFactory;
import org.nasdanika.engineering.EngineeringPackage;
import org.nasdanika.engineering.gen.EngineeringActionProviderAdapterFactory;
import org.nasdanika.engineering.journey.JourneyPackage;
import org.nasdanika.engineering.util.EngineeringYamlSupplier;
import org.nasdanika.exec.ExecPackage;
import org.nasdanika.exec.content.ContentFactory;
import org.nasdanika.exec.content.ContentPackage;
import org.nasdanika.exec.resources.Container;
import org.nasdanika.exec.resources.ReconcileAction;
import org.nasdanika.exec.resources.ResourcesFactory;
import org.nasdanika.exec.resources.ResourcesPackage;
import org.nasdanika.flow.FlowPackage;
import org.nasdanika.html.ecore.EcoreActionSupplier;
import org.nasdanika.html.ecore.EcoreActionSupplierAdapterFactory;
import org.nasdanika.html.ecore.GenModelResourceSet;
import org.nasdanika.html.emf.EObjectActionResolver;
import org.nasdanika.html.jstree.JsTreeFactory;
import org.nasdanika.html.jstree.JsTreeNode;
import org.nasdanika.html.model.app.Action;
import org.nasdanika.html.model.app.AppFactory;
import org.nasdanika.html.model.app.AppPackage;
import org.nasdanika.html.model.app.Label;
import org.nasdanika.html.model.app.Link;
import org.nasdanika.html.model.app.gen.ActionContentProvider;
import org.nasdanika.html.model.app.gen.AppAdapterFactory;
import org.nasdanika.html.model.app.gen.LinkJsTreeNodeSupplierFactoryAdapter;
import org.nasdanika.html.model.app.gen.NavigationPanelConsumerFactoryAdapter;
import org.nasdanika.html.model.app.gen.PageContentProvider;
import org.nasdanika.html.model.app.gen.Util;
import org.nasdanika.html.model.app.util.ActionProvider;
import org.nasdanika.html.model.app.util.AppYamlSupplier;
import org.nasdanika.html.model.bootstrap.BootstrapPackage;
import org.nasdanika.html.model.html.HtmlPackage;
import org.nasdanika.html.model.html.gen.ContentConsumer;
import org.nasdanika.ncore.NcorePackage;
import org.nasdanika.ncore.util.NcoreResourceSet;
import org.nasdanika.resources.BinaryEntityContainer;
import org.nasdanika.resources.FileSystemContainer;

import com.redfin.sitemapgenerator.ChangeFreq;
import com.redfin.sitemapgenerator.WebSitemapGenerator;
import com.redfin.sitemapgenerator.WebSitemapUrl;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;

public class DocGenerator {
	private static final File GENERATED_MODELS_BASE_DIR = new File("target/model-doc");
	private static final File ENGINEERING_MODELS_DIR = new File(GENERATED_MODELS_BASE_DIR, "models");
	private static final File ACTION_MODELS_DIR = new File(GENERATED_MODELS_BASE_DIR, "actions");
	private static final File RESOURCE_MODELS_DIR = new File(GENERATED_MODELS_BASE_DIR, "resources");
	
	private static final URI ENGINEERING_MODELS_URI = URI.createFileURI(ENGINEERING_MODELS_DIR.getAbsolutePath() + "/");	
	private static final URI ACTION_MODELS_URI = URI.createFileURI(ACTION_MODELS_DIR.getAbsolutePath() + "/");	
	private static final URI RESOURCE_MODELS_URI = URI.createFileURI(RESOURCE_MODELS_DIR.getAbsolutePath() + "/");	
	
	/**
	 * Loads a model from YAML, creates a copy and stores to XMI.
	 * @param name
	 * @param progressMonitor
	 * @throws Exception
	 */
	protected void generateEngineeringModel(String name, Context context, ProgressMonitor progressMonitor) throws Exception {
		URI resourceURI = URI.createFileURI(new File("engineering/" + name + ".yml").getAbsolutePath());
		@SuppressWarnings("resource")
		Supplier<EObject> engineeringSupplier = new EngineeringYamlSupplier(resourceURI, context);
		org.nasdanika.common.Consumer<EObject> engineeringConsumer = new org.nasdanika.common.Consumer<EObject>() {

			@Override
			public double size() {
				return 1;
			}

			@Override
			public String name() {
				return "Saving loaded engineering model";
			}

			@Override
			public void execute(EObject obj, ProgressMonitor progressMonitor) {
				EObject copy = EcoreUtil.copy(obj);
				ResourceSet resourceSet = new NcoreResourceSet();
				resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(org.eclipse.emf.ecore.resource.Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
				
				org.eclipse.emf.ecore.resource.Resource instanceModelResource = resourceSet.createResource(URI.createURI(name + ".xml").resolve(ENGINEERING_MODELS_URI));
				instanceModelResource.getContents().add(copy);
				
				org.eclipse.emf.common.util.Diagnostic copyDiagnostic = org.nasdanika.emf.EmfUtil.resolveClearCacheAndDiagnose(resourceSet, context);
				int severity = copyDiagnostic.getSeverity();
				if (severity != org.eclipse.emf.common.util.Diagnostic.OK) {
					EmfUtil.dumpDiagnostic(copyDiagnostic, 2, System.err);
					throw new ExecutionException(new org.eclipse.emf.common.util.DiagnosticException(copyDiagnostic));
				}
				try {
					instanceModelResource.save(null);
				} catch (IOException e) {
					throw new ExecutionException(e, this);
				}
			}
			
		};
		
		try {
			org.nasdanika.common.Diagnostic diagnostic = org.nasdanika.common.Util.call(engineeringSupplier.then(engineeringConsumer), progressMonitor);
			if (diagnostic.getStatus() == Status.FAIL || diagnostic.getStatus() == Status.ERROR) {
				System.err.println("***********************");
				System.err.println("*      Diagnostic     *");
				System.err.println("***********************");
				diagnostic.dump(System.err, 4, Status.FAIL, Status.ERROR);
			}
			if (diagnostic.getStatus() != Status.SUCCESS) {
				throw new DiagnosticException(diagnostic);
			}
			
			if (diagnostic.getStatus() == Status.WARNING || diagnostic.getStatus() == Status.ERROR) {
				System.err.println("***********************");
				System.err.println("*      Diagnostic     *");
				System.err.println("***********************");
				diagnostic.dump(System.err, 4, Status.ERROR, Status.WARNING);
			}
		} catch (DiagnosticException e) {
			System.err.println("******************************");
			System.err.println("*      Diagnostic failed     *");
			System.err.println("******************************");
			e.getDiagnostic().dump(System.err, 4, Status.FAIL);
			throw e;
		}
	}
	
	private DiagramGenerator createDiagramGenerator(ProgressMonitor progressMonitor) {
//		FileSystemContainer output = new FileSystemContainer(new File("target\\diagram-cache"));
//		
//		BiFunction<String,InputStream,String> decoder = (path, state) -> DefaultConverter.INSTANCE.convert(state, String.class);
//		BiFunction<String,String,InputStream> encoder = (path, state) -> DefaultConverter.INSTANCE.convert(state, InputStream.class);
		
		DiagramGenerator plantUMLGenerator = new DiagramGenerator() {
			
			@Override
			public boolean isSupported(String dialect) {
				return DiagramGenerator.UML_DIALECT.equals(dialect);
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
			if (isSameURI(ePackage, FlowPackage.eINSTANCE)) {
				return "core/modules/flow";
			}
			if (isSameURI(ePackage, DiagramPackage.eINSTANCE)) {
				return "core/modules/diagram/modules/model";
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

			if (isSameURI(ePackage, EngineeringPackage.eINSTANCE)) {
				return "engineering/modules/model";
			}
			
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
		
		MutableContext mutableContext = context.fork();
		
		DiagramGenerator diagramGenerator = createDiagramGenerator(progressMonitor);
		mutableContext.register(DiagramGenerator.class, diagramGenerator);//DiagramGenerator.createClient(new URL("http://localhost:8090/spring-exec/api/v1/exec/diagram/")));
		
		ecoreModelsResourceSet.getAdapterFactories().add(new EcoreActionSupplierAdapterFactory(mutableContext, getEPackagePath, org.nasdanika.common.Util.createNasdanikaJavadocResolver(new File(".."), progressMonitor)) {
			
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
				case DiagramPackage.eNS_URI:
					return DiagramPackage.eINSTANCE;
				case ExecPackage.eNS_URI:
					return ExecPackage.eINSTANCE;
				case ContentPackage.eNS_URI:
					return ContentPackage.eINSTANCE;
				case ResourcesPackage.eNS_URI:
					return ResourcesPackage.eINSTANCE;
				case FlowPackage.eNS_URI:
					return FlowPackage.eINSTANCE;
				case NcorePackage.eNS_URI:
					return NcorePackage.eINSTANCE;
				
				case HtmlPackage.eNS_URI:
					return HtmlPackage.eINSTANCE;
				case BootstrapPackage.eNS_URI:
					return BootstrapPackage.eINSTANCE;
				case AppPackage.eNS_URI:
					return AppPackage.eINSTANCE;
				
				case EngineeringPackage.eNS_URI:
					return EngineeringPackage.eINSTANCE;
				case JourneyPackage.eNS_URI:
					return JourneyPackage.eINSTANCE;
				default:
					return super.getEPackage(nsURI);
				}
			}
			
		});
		
		// Physical location relative to the projects (git) root folder -> logical (workspace) name 
		Map<String,String> bundleMap = new LinkedHashMap<>();
		
		bundleMap.put("core/diagram", "org.nasdanika.diagram");
		bundleMap.put("core/exec", "org.nasdanika.exec");
		bundleMap.put("core/flow", "org.nasdanika.flow");
		bundleMap.put("core/ncore", "org.nasdanika.ncore");
				
		bundleMap.put("html/model/app", "org.nasdanika.html.model.app");
		bundleMap.put("html/model/bootstrap", "org.nasdanika.html.model.bootstrap");
		bundleMap.put("html/model/html", "org.nasdanika.html.model.html");
		
		bundleMap.put("engineering/model", "org.nasdanika.engineering");
	
		File modelDir = new File("target/models").getAbsoluteFile();
		modelDir.mkdirs();
		
		File modelDocActionsDir = new File("target/model-doc/actions").getAbsoluteFile();
		delete(modelDocActionsDir);
		modelDocActionsDir.mkdirs();
		
		Map<URI,File> modelToActionModelMap = new LinkedHashMap<>();
		
		File projectsRoot = new File("..");		
		for (Entry<String, String> be: bundleMap.entrySet()) {					
			File sourceDir = new File(projectsRoot, be.getKey());
			File targetDir = new File(modelDir, be.getValue());
			copy(new File(sourceDir, "model"), new File(targetDir, "model"), true, (source, target) -> {
				if (target.getName().endsWith(".genmodel")) {
					modelToActionModelMap.put(URI.createFileURI(target.getAbsolutePath()), new File(modelDocActionsDir, target.getName() + ".xml"));
				}
			});			
			copy(new File(sourceDir, "doc"), new File(targetDir, "doc"), true, null);
		}		
		
//		// Ecore genmodel
//		URL eCoreGenmodelURL = getClass().getResource("/model/Ecore.genmodel");
//		URI eCoreGenmodelURI = URI.createURI(eCoreGenmodelURL.toString());
//		ecoreModelsResourceSet.getResource(eCoreGenmodelURI, true);
		
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
		
	public static void copy(File source, File target, boolean cleanTarget, BiConsumer<File,File> listener) throws IOException {
		if (cleanTarget && target.isDirectory()) {
			delete(target.listFiles());
		}
		if (source.isDirectory()) {
			target.mkdirs();
			for (File sc: source.listFiles()) {
				copy(sc, new File(target, sc.getName()), false, listener);
			}
		} else if (source.isFile()) {
			Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);			
			if (listener != null) {
				listener.accept(source, target);
			}
		}
	}
	
	public static void delete(File... files) {
		for (File file: files) {
			if (file.exists()) {
				if (file.isDirectory()) {
					delete(file.listFiles());
				}
				file.delete();
			}
		}
	}
		
	public static void copy(File source, File target, boolean cleanTarget, Predicate<String> cleanPredicate, BiConsumer<File,File> listener) throws IOException {
		if (cleanTarget && target.isDirectory()) {
			delete(null, cleanPredicate, target.listFiles());
		}
		if (source.isDirectory()) {
			target.mkdirs();
			for (File sc: source.listFiles()) {
				copy(sc, new File(target, sc.getName()), false, listener);
			}
		} else if (source.isFile()) {
			Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);			
			if (listener != null) {
				listener.accept(source, target);
			}
		}
	}
	
	public static void delete(String path, Predicate<String> deletePredicate, File... files) {
		for (File file: files) {
			String filePath = path == null ? file.getName() : path + "/" + file.getName();
			if (file.exists() && (deletePredicate == null || deletePredicate.test(filePath))) {
				if (file.isDirectory()) {
					delete(filePath, deletePredicate, file.listFiles());
				}
				file.delete();
			}
		}
	}	
		
	/**
	 * Loads instance model from previously generated XMI, diagnoses, generates action model.
	 * @throws Exception
	 */
	public Map<EObject,Action>  generateActionModel(String name, Context context, ProgressMonitor progressMonitor) throws Exception {
		ResourceSet instanceModelsResourceSet = createResourceSet(context, progressMonitor);
		Resource instanceModelResource = instanceModelsResourceSet.getResource(URI.createURI(name + ".xml").resolve(ENGINEERING_MODELS_URI), true);

		org.eclipse.emf.common.util.Diagnostic instanceDiagnostic = org.nasdanika.emf.EmfUtil.resolveClearCacheAndDiagnose(instanceModelsResourceSet, context);
		int severity = instanceDiagnostic.getSeverity();
		if (severity != org.eclipse.emf.common.util.Diagnostic.OK) {
			EmfUtil.dumpDiagnostic(instanceDiagnostic, 2, System.err);
			throw new org.eclipse.emf.common.util.DiagnosticException(instanceDiagnostic);
		}
		
		instanceModelsResourceSet.getAdapterFactories().add(new EngineeringActionProviderAdapterFactory(context) {
			
			private void collect(Notifier target, org.eclipse.emf.common.util.Diagnostic source, Collection<org.eclipse.emf.common.util.Diagnostic> accumulator) {
				List<?> data = source.getData();
				if (source.getChildren().isEmpty()
						&& source.getSeverity() > org.eclipse.emf.common.util.Diagnostic.OK 
						&& data != null 
						&& data.size() == 1 
						&& data.get(0) == target) {
					accumulator.add(source);
				}
				for (org.eclipse.emf.common.util.Diagnostic child: source.getChildren()) {
					collect(target, child, accumulator);
				}
			}
			
			protected Collection<org.eclipse.emf.common.util.Diagnostic> getDiagnostic(Notifier target) {
				Collection<org.eclipse.emf.common.util.Diagnostic> ret = new ArrayList<>();
				collect(target, instanceDiagnostic, ret);
				return ret;
			}
			
			private void collect(Notifier target, EStructuralFeature feature, org.eclipse.emf.common.util.Diagnostic source, Collection<org.eclipse.emf.common.util.Diagnostic> accumulator) {
				List<?> data = source.getData();
				if (source.getChildren().isEmpty() 
						&& source.getSeverity() > org.eclipse.emf.common.util.Diagnostic.OK 
						&& data != null 
						&& data.size() > 1 
						&& data.get(0) == target 
						&& data.get(1) == feature) {
					accumulator.add(source);
				}
				for (org.eclipse.emf.common.util.Diagnostic child: source.getChildren()) {
					collect(target, feature, child, accumulator);
				}
			}

			protected Collection<org.eclipse.emf.common.util.Diagnostic> getFeatureDiagnostic(Notifier target, EStructuralFeature feature) {
				Collection<org.eclipse.emf.common.util.Diagnostic> ret = new ArrayList<>();
				collect(target, feature, instanceDiagnostic, ret);
				return ret;
			}
			
		});
		
		ResourceSet actionModelsResourceSet = createResourceSet(context, progressMonitor);
		actionModelsResourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(org.eclipse.emf.ecore.resource.Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
		
		org.eclipse.emf.ecore.resource.Resource actionModelResource = actionModelsResourceSet.createResource(URI.createURI(name + ".xml").resolve(ACTION_MODELS_URI));
		
		Map<EObject,Action> registry = new HashMap<>();
		EObject instance = instanceModelResource.getContents().get(0);
		Action rootAction = EObjectAdaptable.adaptTo(instance, ActionProvider.class).execute(registry::put, progressMonitor);
		Context uriResolverContext = Context.singleton(Context.BASE_URI_PROPERTY, URI.createURI("temp://" + UUID.randomUUID() + "/" + UUID.randomUUID() + "/"));
		BiFunction<Label, URI, URI> uriResolver = org.nasdanika.html.model.app.util.Util.uriResolver(rootAction, uriResolverContext);
		Adapter resolver = EcoreUtil.getExistingAdapter(rootAction, EObjectActionResolver.class);
		if (resolver instanceof EObjectActionResolver) {														
			org.nasdanika.html.emf.EObjectActionResolver.Context resolverContext = new org.nasdanika.html.emf.EObjectActionResolver.Context() {

				@Override
				public Action getAction(EObject semanticElement) {
					return registry.get(semanticElement);
				}

				@Override
				public URI resolve(Action action, URI base) {
					return uriResolver.apply(action, base);
				}
				
			};
			((EObjectActionResolver) resolver).execute(resolverContext, progressMonitor);
		}
		actionModelResource.getContents().add(rootAction);

		actionModelResource.save(null);
		
		return registry;
	}
	
	protected EObject loadObject(
			String resource, 
			Consumer<org.nasdanika.common.Diagnostic> diagnosticConsumer,
			Context context,
			ProgressMonitor progressMonitor) throws Exception {
		
		URI resourceURI = URI.createFileURI(new File(resource).getAbsolutePath());
				
		// Diagnosing loaded resources. 
		try {
			return Objects.requireNonNull(org.nasdanika.common.Util.call(new AppYamlSupplier(resourceURI, context), progressMonitor, diagnosticConsumer), "Loaded null from " + resource);
		} catch (DiagnosticException e) {
			System.err.println("******************************");
			System.err.println("*      Diagnostic failed     *");
			System.err.println("******************************");
			e.getDiagnostic().dump(System.err, 4, Status.FAIL);
			throw e;
		}		
	}
	
	/**
	 * Generates a resource model from an action model.
	 * @throws Exception
	 */
	public void generateResourceModel(String name, Map<EObject, Action> registry, Context context, ProgressMonitor progressMonitor) throws Exception {
		java.util.function.Consumer<Diagnostic> diagnosticConsumer = diagnostic -> {
			if (diagnostic.getStatus() == Status.FAIL || diagnostic.getStatus() == Status.ERROR) {
				System.err.println("***********************");
				System.err.println("*      Diagnostic     *");
				System.err.println("***********************");
				diagnostic.dump(System.err, 4, Status.FAIL, Status.ERROR);
			}
			if (diagnostic.getStatus() != Status.SUCCESS) {
				throw new DiagnosticException(diagnostic);
			};
		};
		
		Context modelContext = Context.singleton("model-name", name);
		String actionsResource = "engineering/actions.yml";
		Action root = (Action) Objects.requireNonNull(loadObject(actionsResource, diagnosticConsumer, modelContext, progressMonitor), "Loaded null from " + actionsResource);
		root.eResource().getResourceSet().getAdapterFactories().add(new AppAdapterFactory());
		
		Container container = ResourcesFactory.eINSTANCE.createContainer();
		container.setName(name);
		container.setReconcileAction(ReconcileAction.OVERWRITE);
		
		ResourceSet resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
		Resource modelResource = resourceSet.createResource(URI.createURI(name + ".xml").resolve(RESOURCE_MODELS_URI));
		modelResource.getContents().add(container);
		
		String pageTemplateResource = "engineering/page-template.yml";
		org.nasdanika.html.model.bootstrap.Page pageTemplate = (org.nasdanika.html.model.bootstrap.Page) Objects.requireNonNull(loadObject(pageTemplateResource, diagnosticConsumer, modelContext, progressMonitor), "Loaded null from " + pageTemplateResource);
		
		File contentDir = new File(RESOURCE_MODELS_DIR, "content");
		contentDir.mkdirs();
		// Generating content file from action content 
		ActionContentProvider.Factory actionContentProviderFactory = (contentProviderContext) -> (action, uriResolver, pMonitor) -> {
			
			@SuppressWarnings("unchecked")
			java.util.function.Function<Context, String> siteMapTreeScriptComputer = ctx -> {
				// TODO - actions from action prototype, e.g. Ecore doc actions, to the tree.
				
				JsTreeFactory jsTreeFactory = context.get(JsTreeFactory.class, JsTreeFactory.INSTANCE);
				Map<EObject, JsTreeNode> nodeMap = new HashMap<>();
				for (Entry<EObject, Action> re: registry.entrySet()) {
					Action treeAction = re.getValue();
					
					Link link = AppFactory.eINSTANCE.createLink();
					String treeActionText = treeAction.getText();
					int maxLength = 50;
					link.setText(treeActionText.length() > maxLength ? treeActionText.substring(0, maxLength) + "..." : treeActionText);
					link.setIcon(treeAction.getIcon());
					
					URI bURI = uriResolver.apply(action, (URI) null);
					URI tURI = uriResolver.apply(treeAction, bURI);
					link.setLocation(tURI.toString());
					LinkJsTreeNodeSupplierFactoryAdapter<Link> adapter = new LinkJsTreeNodeSupplierFactoryAdapter<>(link);
					
					try {
						JsTreeNode jsTreeNode = adapter.create(ctx).execute(progressMonitor);
						jsTreeNode.attribute(Util.DATA_NSD_ACTION_UUID_ATTRIBUTE, treeAction.getUuid());
						nodeMap.put(re.getKey(), jsTreeNode);
					} catch (Exception e) {
						throw new NasdanikaException(e);
					}
				}
				
				Map<EObject, JsTreeNode> roots = new HashMap<>(nodeMap);
				
				Map<EObject,Map<String,List<JsTreeNode>>> refMap = new HashMap<>();
				for (EObject eObj: new ArrayList<>(nodeMap.keySet())) {
					Map<String,List<JsTreeNode>> rMap = new TreeMap<>();					
					for (EReference eRef: eObj.eClass().getEAllReferences()) {
						if (eRef.isContainment()) {
							Object eRefValue = eObj.eGet(eRef);
							List<JsTreeNode> refNodes = new ArrayList<>();
							for (Object ve: eRefValue instanceof Collection ? (Collection<Object>) eRefValue : Collections.singletonList(eRefValue)) {
								JsTreeNode refNode = roots.remove(ve);
								if (refNode != null) {
									refNodes.add(refNode);
								}
							}
							if (!refNodes.isEmpty()) {
								rMap.put(org.nasdanika.common.Util.nameToLabel(eRef.getName()) , refNodes);
							}
						}
					}
					if (!rMap.isEmpty()) {
						refMap.put(eObj, rMap);
					}
				}
				
				for (Entry<EObject, JsTreeNode> ne: nodeMap.entrySet()) {
					Map<String, List<JsTreeNode>> refs = refMap.get(ne.getKey());
					if (refs != null) {
						for (Entry<String, List<JsTreeNode>> ref: refs.entrySet()) {
							JsTreeNode refNode = jsTreeFactory.jsTreeNode();
							refNode.text(ref.getKey());
							refNode.children().addAll(ref.getValue());
							ne.getValue().children().add(refNode);
						}
					}
				}
				
				JSONObject jsTree = jsTreeFactory.buildJsTree(roots.values());
		
				List<String> plugins = new ArrayList<>();
				plugins.add("state");
				plugins.add("search");
				JSONObject searchConfig = new JSONObject();
				searchConfig.put("show_only_matches", true);
				jsTree.put("search", searchConfig);
				jsTree.put("plugins", plugins); 		
				jsTree.put("state", Collections.singletonMap("key", "nsd-site-map-tree"));
				
				// Deletes selection from state
				String filter = NavigationPanelConsumerFactoryAdapter.CLEAR_STATE_FILTER + " tree.search.search_callback = (results, node) => results.split(' ').includes(node.original['data-nsd-action-uuid']);";
				
				return jsTreeFactory.bind("#nsd-site-map-tree", jsTree, filter, null).toString();				
			};			
			MutableContext mctx = contentProviderContext.fork();
			mctx.put("nsd-site-map-tree-script", siteMapTreeScriptComputer);
			
			List<Object> contentContributions = new ArrayList<>();
			mctx.register(ContentConsumer.class, (ContentConsumer) contentContributions::add);			
			
			String fileName = action.getUuid() + ".html";
			SupplierFactory<InputStream> contentFactory = org.nasdanika.common.Util.asInputStreamSupplierFactory(action.getContent());			
			try (InputStream contentStream = org.nasdanika.common.Util.call(contentFactory.create(mctx), pMonitor, diagnosticConsumer, Status.FAIL, Status.ERROR)) {
				if (contentContributions.isEmpty()) {
					Files.copy(contentStream, new File(contentDir, fileName).toPath(), StandardCopyOption.REPLACE_EXISTING);
				} else {
					Stream<InputStream> pageBodyContributionsStream = contentContributions.stream().filter(Objects::nonNull).map(e -> {
						try {
							return DefaultConverter.INSTANCE.toInputStream(e.toString());
						} catch (IOException ex) {
							throw new NasdanikaException("Error converting element to InputStream: " + ex, ex);
						}
					});
					Stream<InputStream> concatenatedStream = Stream.concat(pageBodyContributionsStream, Stream.of(contentStream));
					Files.copy(org.nasdanika.common.Util.join(concatenatedStream), new File(contentDir, fileName).toPath(), StandardCopyOption.REPLACE_EXISTING);
				}
			} catch (Exception e) {
				throw new NasdanikaException(e);
			}
			
			org.nasdanika.exec.content.Resource contentResource = ContentFactory.eINSTANCE.createResource();
			contentResource.setLocation("../content/" + fileName);
			System.out.println("[Action content] " + action.getName() + " -> " + fileName);
			return ECollections.singletonEList(contentResource);			
		};
		
		File pagesDir = new File(RESOURCE_MODELS_DIR, "pages");
		pagesDir.mkdirs();
		PageContentProvider.Factory pageContentProviderFactory = (contentProviderContext) -> (page, baseURI, uriResolver, pMonitor) -> {
			try {
				// Saving a page to .xml and creating a reference to .html; Pages shall be processed from .xml to .html individually.
				page.setUuid(UUID.randomUUID().toString());
				
				ResourceSet pageResourceSet = new ResourceSetImpl();
				pageResourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());			
				URI pageURI = URI.createFileURI(new File(pagesDir, page.getUuid() + ".xml").getCanonicalPath());
				Resource pageEResource = pageResourceSet.createResource(pageURI);
				pageEResource.getContents().add(page);
				pageEResource.save(null);
				
				org.nasdanika.exec.content.Resource pageResource = ContentFactory.eINSTANCE.createResource();
				pageResource.setLocation("pages/" + page.getUuid() + ".html");
				System.out.println("[Page content] " + page.getName() + " -> " + pageResource.getLocation());
				return pageResource;
			} catch (IOException e) {
				throw new NasdanikaException(e);
			}
		};
		
		Util.generateSite(
				root, 
				pageTemplate,
				container,
				actionContentProviderFactory,
				pageContentProviderFactory,
				context,
				progressMonitor);
		
		modelResource.save(null);
		
		// Page model to XML conversion
		ResourceSet pageResourceSet = new ResourceSetImpl();
		pageResourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());			
		pageResourceSet.getAdapterFactories().add(new AppAdapterFactory());
		for (File pageFile: pagesDir.listFiles(f -> f.getName().endsWith(".xml"))) {
			URI pageURI = URI.createFileURI(pageFile.getCanonicalPath());
			Resource pageEResource = pageResourceSet.getResource(pageURI, true);
			SupplierFactory<InputStream> contentFactory = org.nasdanika.common.Util.asInputStreamSupplierFactory(pageEResource.getContents());			
			try (InputStream contentStream = org.nasdanika.common.Util.call(contentFactory.create(context), progressMonitor, diagnosticConsumer, Status.FAIL, Status.ERROR)) {
				Files.copy(contentStream, new File(pageFile.getCanonicalPath().replace(".xml", ".html")).toPath(), StandardCopyOption.REPLACE_EXISTING);
				System.out.println("[Page xml -> html] " + pageFile.getName());
			}
		}				
	}
	
	private void copyJavaDoc() throws Exception {
		copy(new File("../core/target/site/apidocs"), new File("docs/modules/core/apidocs"), false, null, null);		
		copy(new File("../html/target/site/apidocs"), new File("docs/modules/html/apidocs"), false, null, null);	
		copy(new File("../engineering/target/site/apidocs"), new File("docs/modules/engineering/apidocs"), false, null, null);	
	}
	
	/**
	 * Generates files from the previously generated resource model.
	 * @throws Exception
	 */
	public void generateContainer(String name, Context context, ProgressMonitor progressMonitor) throws Exception {
		ResourceSet resourceSet = createResourceSet(context, progressMonitor);
		
		resourceSet.getAdapterFactories().add(new AppAdapterFactory());
				
		Resource containerResource = resourceSet.getResource(URI.createURI(name + ".xml").resolve(RESOURCE_MODELS_URI), true);
	
		File siteDir = new File("target/model-doc/site");
		BinaryEntityContainer container = new FileSystemContainer(siteDir);
		for (EObject eObject : containerResource.getContents()) {
			Diagnostician diagnostician = new Diagnostician();
			org.eclipse.emf.common.util.Diagnostic diagnostic = diagnostician.validate(eObject);
			if (diagnostic.getSeverity() == org.eclipse.emf.common.util.Diagnostic.ERROR) {
				throw new org.eclipse.emf.common.util.DiagnosticException(diagnostic);
			};
			// Diagnosing loaded resources. 
			try {
				ConsumerFactory<BinaryEntityContainer> consumerFactory = Objects.requireNonNull(EObjectAdaptable.adaptToConsumerFactory(eObject, BinaryEntityContainer.class), "Cannot adapt to ConsumerFactory");
				Diagnostic callDiagnostic = org.nasdanika.common.Util.call(consumerFactory.create(context), container, progressMonitor);
				if (callDiagnostic.getStatus() == Status.FAIL || callDiagnostic.getStatus() == Status.ERROR) {
					System.err.println("***********************");
					System.err.println("*      Diagnostic     *");
					System.err.println("***********************");
					callDiagnostic.dump(System.err, 4, Status.FAIL, Status.ERROR);
				}
				if (callDiagnostic.getStatus() != Status.SUCCESS) {
					throw new DiagnosticException(callDiagnostic);
				};
			} catch (DiagnosticException e) {
				System.err.println("******************************");
				System.err.println("*      Diagnostic failed     *");
				System.err.println("******************************");
				e.getDiagnostic().dump(System.err, 4, Status.FAIL);
				throw e;
			}
		}
		
		// Cleanup docs, keep CNAME, favicon.ico, and images directory. Copy from target/model-doc/site/nasdanika
		Predicate<String> cleanPredicate = path -> {
			return !"CNAME".equals(path) && !"favicon.ico".equals(path) && !path.startsWith("images/");
		};

		File docsDir = new File("docs");
		copy(new File(siteDir, "nasdanika"), docsDir, true, cleanPredicate, null);
		
		copyJavaDoc();
		
		generateSitemapAndSearch(docsDir);
	}

	private void generateSitemapAndSearch(File docsDir) throws IOException {
		int[] problems = { 0 };
		
		// Site map and search index
		JSONObject searchDocuments = new JSONObject();		
		String domain = "https://docs.nasdanika.org";
		WebSitemapGenerator wsg = new WebSitemapGenerator(domain, docsDir);
		BiConsumer<File, String> listener = new BiConsumer<File, String>() {
			
			@Override
			public void accept(File file, String path) {
				if (path.endsWith(".html")) {
					try {
						WebSitemapUrl url = new WebSitemapUrl.Options(domain + "/" + path)
							    .lastMod(new Date(file.lastModified())).changeFreq(ChangeFreq.WEEKLY).build();
						wsg.addUrl(url); 
					} catch (MalformedURLException e) {
						throw new NasdanikaException(e);
					}
					
					// Excluding search.html and aggregator pages which contain information present elsewhere
					if (!"search.html".equals(path)
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
							&& !path.endsWith("-all-supertypes.html")) {

						try {
							Predicate<String> predicate = org.nasdanika.html.model.app.gen.Util.createRelativeLinkPredicate(file, docsDir);						
							Consumer<? super Element> inspector = org.nasdanika.html.model.app.gen.Util.createInspector(predicate, error -> {
								System.err.println("[" + path +"] " + error);
								++problems[0];
							});
							
							JSONObject searchDocument = org.nasdanika.html.model.app.gen.Util.createSearchDocument(path, file, inspector, DocGenerator.this::configureSearch);
							if (searchDocument != null) {
								searchDocuments.put(path, searchDocument);
							}
						} catch (IOException e) {
							throw new NasdanikaException(e);
						}
					}
				}
			}
		};
		org.nasdanika.common.Util.walk(null, listener, docsDir.listFiles());
		wsg.write();	

		try (FileWriter writer = new FileWriter(new File(docsDir, "search-documents.js"))) {
			writer.write("var searchDocuments = " + searchDocuments);
		}
		
		if (problems[0] != 76) {
			throw new ExecutionException("There are problems with pages: " + problems[0]);
		};
	}
	
	protected boolean configureSearch(String path, Document doc) {
		Element head = doc.head();
		URI base = URI.createURI("temp://" + UUID.randomUUID() + "/");
		URI searchScriptURI = URI.createURI("search-documents.js").resolve(base);
		URI thisURI = URI.createURI(path).resolve(base);
		URI relativeSearchScriptURI = searchScriptURI.deresolve(thisURI, true, true, true);
		head.append(System.lineSeparator() + "<script src=\"" + relativeSearchScriptURI + "\"></script>" + System.lineSeparator());
		head.append(System.lineSeparator() + "<script src=\"https://unpkg.com/lunr/lunr.js\"></script>" + System.lineSeparator());
				
		try (InputStream in = new FileInputStream("engineering/search.js")) {
			head.append(System.lineSeparator() + "<script>" + System.lineSeparator() + DefaultConverter.INSTANCE.toString(in) + System.lineSeparator() + "</script>" + System.lineSeparator());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return true;
	}
	
	protected ResourceSet createResourceSet(Context context, ProgressMonitor progressMonitor) {
		// Load model from XMI
		ResourceSet resourceSet = new NcoreResourceSet();
		Map<String, Object> extensionToFactoryMap = resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap();
		extensionToFactoryMap.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
		
		YamlResourceFactory yamlResourceFactory = new YamlResourceFactory(new EObjectLoader(null, null, resourceSet), context, progressMonitor);
		extensionToFactoryMap.put("yml", yamlResourceFactory);
	
		resourceSet.getPackageRegistry().put(NcorePackage.eNS_URI, NcorePackage.eINSTANCE);
		resourceSet.getPackageRegistry().put(DiagramPackage.eNS_URI, DiagramPackage.eINSTANCE);
		resourceSet.getPackageRegistry().put(ExecPackage.eNS_URI, ExecPackage.eINSTANCE);
		resourceSet.getPackageRegistry().put(ContentPackage.eNS_URI, ContentPackage.eINSTANCE);
		resourceSet.getPackageRegistry().put(ResourcesPackage.eNS_URI, ResourcesPackage.eINSTANCE);
		resourceSet.getPackageRegistry().put(HtmlPackage.eNS_URI, HtmlPackage.eINSTANCE);
		resourceSet.getPackageRegistry().put(BootstrapPackage.eNS_URI, BootstrapPackage.eINSTANCE);
		resourceSet.getPackageRegistry().put(AppPackage.eNS_URI, AppPackage.eINSTANCE);
		resourceSet.getPackageRegistry().put(FlowPackage.eNS_URI, FlowPackage.eINSTANCE);
		resourceSet.getPackageRegistry().put(EngineeringPackage.eNS_URI, EngineeringPackage.eINSTANCE);
		
//		resourceSet.getAdapterFactories().add(new AppAdapterFactory())				
		return resourceSet;
	}
	
	public void generate() throws Exception {
		delete(ENGINEERING_MODELS_DIR);
		delete(ACTION_MODELS_DIR);
		delete(RESOURCE_MODELS_DIR);
		
		ENGINEERING_MODELS_DIR.mkdirs();
		ACTION_MODELS_DIR.mkdirs();
		RESOURCE_MODELS_DIR.mkdirs();

		ProgressMonitor progressMonitor = new NullProgressMonitor(); // PrintStreamProgressMonitor();		

		Function<String, String> nasdanikaResolver = org.nasdanika.common.Util.createNasdanikaJavadocResolver(new File(".."), progressMonitor);
		
		MutableContext context = Context.EMPTY_CONTEXT.fork();
		context.put("javadoc", org.nasdanika.common.Util.createJavadocPropertyComputer(nasdanikaResolver));
		
		long start = System.currentTimeMillis();
		generateEcoreActionModel(context, progressMonitor);
		System.out.println("\tGenerated ecore action model in " + (System.currentTimeMillis() - start) + " milliseconds");
		start = System.currentTimeMillis();
		
		generateSite("nasdanika", context, progressMonitor);
		
		long cacheMisses = FeatureCacheAdapter.getMisses();
		long cacheCalls = FeatureCacheAdapter.getCalls();
		long cacheEfficiency = 100*(cacheCalls - cacheMisses)/cacheCalls;
		System.out.println("Feature cache - calls: " + cacheCalls + ", misses: " + cacheMisses + ", efficiency: " + cacheEfficiency + "%, compute time: " + TimeUnit.NANOSECONDS.toMillis(FeatureCacheAdapter.getComputeTime()) + " milliseconds.");
	}
	
	private void generateSite(String name, Context context, ProgressMonitor progressMonitor) throws Exception {
		System.out.println("Generating site: " + name);
		
		long start = System.currentTimeMillis();
		generateEngineeringModel(name, context, progressMonitor);
		System.out.println("\tGenerated instance model in " + (System.currentTimeMillis() - start) + " milliseconds");
		start = System.currentTimeMillis();
		
		Map<EObject, Action> registry = generateActionModel(name, context, progressMonitor);
		System.out.println("\tGenerated action model in " + (System.currentTimeMillis() - start) + " milliseconds");
		start = System.currentTimeMillis();
		
		generateResourceModel(name, registry, context, progressMonitor);
		System.out.println("\tGenerated resource model in " + (System.currentTimeMillis() - start) + " milliseconds");
		start = System.currentTimeMillis();
		
		generateContainer(name, context, progressMonitor);
		System.out.println("\tGenerated site in " + (System.currentTimeMillis() - start) + " milliseconds");
	}

}
