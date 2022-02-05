package org.nasdanika.docs.engineering;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.Hex;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.GenPackage;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;
import org.nasdanika.common.ConsumerFactory;
import org.nasdanika.common.Context;
import org.nasdanika.common.DefaultConverter;
import org.nasdanika.common.Diagnostic;
import org.nasdanika.common.DiagnosticException;
import org.nasdanika.common.DiagramGenerator;
import org.nasdanika.common.MutableContext;
import org.nasdanika.common.NasdanikaException;
import org.nasdanika.common.NullProgressMonitor;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.common.Status;
import org.nasdanika.common.Supplier;
import org.nasdanika.common.resources.BinaryEntityContainer;
import org.nasdanika.common.resources.FileSystemContainer;
import org.nasdanika.diagram.DiagramPackage;
import org.nasdanika.emf.EObjectAdaptable;
import org.nasdanika.emf.EmfUtil;
import org.nasdanika.emf.persistence.EObjectLoader;
import org.nasdanika.emf.persistence.FeatureCacheAdapter;
import org.nasdanika.emf.persistence.YamlResourceFactory;
import org.nasdanika.engineering.EngineeringPackage;
import org.nasdanika.engineering.gen.EngineeringActionProviderAdapterFactory;
import org.nasdanika.engineering.util.EngineeringYamlSupplier;
import org.nasdanika.exec.ExecPackage;
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
import org.nasdanika.html.model.app.Action;
import org.nasdanika.html.model.app.AppPackage;
import org.nasdanika.html.model.app.gen.AppAdapterFactory;
import org.nasdanika.html.model.app.gen.AppGenYamlLoadingExecutionParticipant;
import org.nasdanika.html.model.app.gen.Util;
import org.nasdanika.html.model.app.util.ActionProvider;
import org.nasdanika.html.model.bootstrap.BootstrapPackage;
import org.nasdanika.html.model.html.HtmlPackage;
import org.nasdanika.ncore.NcorePackage;
import org.nasdanika.ncore.util.NcoreResourceSet;

import com.redfin.sitemapgenerator.ChangeFreq;
import com.redfin.sitemapgenerator.WebSitemapGenerator;
import com.redfin.sitemapgenerator.WebSitemapUrl;

/**
 * Tests of agile flows.
 * @author Pavel
 *
 */
public class TestNasdanikaDocEngineeringGen /* extends TestBase */ {
	
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
			public void execute(EObject obj, ProgressMonitor progressMonitor) throws Exception {
				EObject copy = EcoreUtil.copy(obj);
				ResourceSet resourceSet = new NcoreResourceSet();
				resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(org.eclipse.emf.ecore.resource.Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
				
				org.eclipse.emf.ecore.resource.Resource instanceModelResource = resourceSet.createResource(URI.createURI(name + ".xml").resolve(ENGINEERING_MODELS_URI));
				instanceModelResource.getContents().add(copy);
				
				org.eclipse.emf.common.util.Diagnostic copyDiagnostic = org.nasdanika.emf.EmfUtil.resolveClearCacheAndDiagnose(resourceSet, context);
				int severity = copyDiagnostic.getSeverity();
				if (severity != org.eclipse.emf.common.util.Diagnostic.OK) {
					EmfUtil.dumpDiagnostic(copyDiagnostic, 2, System.err);
				}
				assertThat(severity).isEqualTo(org.eclipse.emf.common.util.Diagnostic.OK);
				instanceModelResource.save(null);
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
			assertThat(diagnostic.getStatus()).isEqualTo(Status.SUCCESS);
			
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
		FileSystemContainer output = new FileSystemContainer(new File("target\\diagram-cache"));
		
		BiFunction<String,InputStream,String> decoder = (path, state) -> DefaultConverter.INSTANCE.convert(state, String.class);
		BiFunction<String,String,InputStream> encoder = (path, state) -> DefaultConverter.INSTANCE.convert(state, InputStream.class);
		return DiagramGenerator.INSTANCE.cachingDiagramGenerator(output.stateAdapter().adapt(decoder, encoder), progressMonitor);
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
		
		ecoreModelsResourceSet.getAdapterFactories().add(new EcoreActionSupplierAdapterFactory(mutableContext, getEPackagePath, org.nasdanika.common.Util.createNasdanikaJavadocResolver(new File(".."), progressMonitor)));
		
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
	 * Walks the directory passing files to the listener.
	 * @param source
	 * @param target
	 * @param cleanTarget
	 * @param cleanPredicate
	 * @param listener
	 * @throws IOException
	 */
	public static void walk(String path, BiConsumer<File,String> listener, File... files) throws IOException {
		for (File file: files) {
			String filePath = path == null ? file.getName() : path + "/" + file.getName();
			if (file.isDirectory()) {
				walk(filePath, listener, file.listFiles());
			} else if (file.isFile() && listener != null) {
				listener.accept(file, filePath);
			}
		}
	}
	
		
	/**
	 * Loads instance model from previously generated XMI, diagnoses, generates action model.
	 * @throws Exception
	 */
	public void generateActionModel(String name, Context context, ProgressMonitor progressMonitor) throws Exception {
		ResourceSet instanceModelsResourceSet = createResourceSet(context, progressMonitor);
		Resource instanceModelResource = instanceModelsResourceSet.getResource(URI.createURI(name + ".xml").resolve(ENGINEERING_MODELS_URI), true);

		org.eclipse.emf.common.util.Diagnostic instanceDiagnostic = org.nasdanika.emf.EmfUtil.resolveClearCacheAndDiagnose(instanceModelsResourceSet, context);
		int severity = instanceDiagnostic.getSeverity();
		if (severity != org.eclipse.emf.common.util.Diagnostic.OK) {
			EmfUtil.dumpDiagnostic(instanceDiagnostic, 2, System.err);
		}
		assertThat(severity).isEqualTo(org.eclipse.emf.common.util.Diagnostic.OK);
		
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
		BiFunction<Action, URI, URI> uriResolver = org.nasdanika.html.model.app.gen.Util.uriResolver(rootAction, uriResolverContext);
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
	}
	
	protected EObject loadObject(
			String resource, 
			Consumer<org.nasdanika.common.Diagnostic> diagnosticConsumer,
			Context context,
			ProgressMonitor progressMonitor) throws Exception {
		
		URI resourceURI = URI.createFileURI(new File(resource).getAbsolutePath());

		class ObjectSupplier extends AppGenYamlLoadingExecutionParticipant implements Supplier<EObject> {

			public ObjectSupplier(Context context) {
				super(context);
			}

			@Override
			protected Collection<URI> getResources() {
				return Collections.singleton(resourceURI);
			}

			@Override
			public EObject execute(ProgressMonitor progressMonitor) throws Exception {				
				return resourceSet.getResource(resourceURI, false).getContents().iterator().next();
			}
			
		};
		
		// Diagnosing loaded resources. 
		try {
			return Objects.requireNonNull(org.nasdanika.common.Util.call(new ObjectSupplier(context), progressMonitor, diagnosticConsumer), "Loaded null from " + resource);
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
	public void generateResourceModel(String name, Context context, ProgressMonitor progressMonitor) throws Exception {
		java.util.function.Consumer<Diagnostic> diagnosticConsumer = diagnostic -> {
			if (diagnostic.getStatus() == Status.FAIL || diagnostic.getStatus() == Status.ERROR) {
				System.err.println("***********************");
				System.err.println("*      Diagnostic     *");
				System.err.println("***********************");
				diagnostic.dump(System.err, 4, Status.FAIL, Status.ERROR);
			}
			assertThat(diagnostic.getStatus()).isEqualTo(Status.SUCCESS);
		};
		
		Context modelContext = Context.singleton("model-name", name);
		String actionsResource = "engineering/actions.yml";
		Action root = (Action) Objects.requireNonNull(loadObject(actionsResource, diagnosticConsumer, modelContext, progressMonitor), "Loaded null from " + actionsResource);
		
		Container container = ResourcesFactory.eINSTANCE.createContainer();
		container.setName(name);
		container.setReconcileAction(ReconcileAction.OVERWRITE);
		
		ResourceSet resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
		Resource modelResource = resourceSet.createResource(URI.createURI(name + ".xml").resolve(RESOURCE_MODELS_URI));
		modelResource.getContents().add(container);
		
		String pageTemplateResource = "engineering/page-template.yml";
		org.nasdanika.html.model.bootstrap.Page pageTemplate = (org.nasdanika.html.model.bootstrap.Page) Objects.requireNonNull(loadObject(pageTemplateResource, diagnosticConsumer, modelContext, progressMonitor), "Loaded null from " + pageTemplateResource);
		
		Util.generateSite(
				root, 
				pageTemplate,
				container,
				context,
				progressMonitor);
		
		modelResource.save(null);
	}
	
	private void copyJavaDoc() throws Exception {
		copy(new File("../core/common/target/apidocs"), new File("docs/modules/core/modules/common/apidocs"), false, null, null);
		copy(new File("../core/ncore/target/apidocs"), new File("docs/modules/core/modules/ncore/apidocs"), false, null, null);
		copy(new File("../core/diagram/target/apidocs"), new File("docs/modules/core/modules/diagram/modules/model/apidocs"), false, null, null);
		copy(new File("../core/diagram.gen/target/apidocs"), new File("docs/modules/core/modules/diagram/modules/gen/apidocs"), false, null, null);
		copy(new File("../core/flow/target/apidocs"), new File("docs/modules/core/modules/flow/apidocs"), false, null, null);
		copy(new File("../core/exec/target/apidocs"), new File("docs/modules/core/modules/exec/modules/model/apidocs"), false, null, null);
		copy(new File("../core/exec.gen/target/apidocs"), new File("docs/modules/core/modules/exec/modules/gen/apidocs"), false, null, null);
		copy(new File("../core/cli/target/apidocs"), new File("docs/modules/core/modules/cli/apidocs"), false, null, null);
		copy(new File("../core/emf/target/apidocs"), new File("docs/modules/core/modules/emf/apidocs"), false, null, null);	
		
		copy(new File("../html/html/target/apidocs"), new File("docs/modules/html/modules/html/apidocs"), false, null, null);	
		copy(new File("../html/bootstrap/target/apidocs"), new File("docs/modules/html/modules/bootstrap/apidocs"), false, null, null);	
		copy(new File("../html/jstree/target/apidocs"), new File("docs/modules/html/modules/jstree/apidocs"), false, null, null);	
		copy(new File("../html/emf/target/apidocs"), new File("docs/modules/html/modules/emf/apidocs"), false, null, null);	
		copy(new File("../html/ecore/target/apidocs"), new File("docs/modules/html/modules/ecore/apidocs"), false, null, null);	
		copy(new File("../html/flow/target/apidocs"), new File("docs/modules/html/modules/flow/apidocs"), false, null, null);	

		copy(new File("../html/model/html/target/apidocs"), new File("docs/modules/html/modules/models/modules/html/modules/model/apidocs"), false, null, null);	
		copy(new File("../html/model/html.gen/target/apidocs"), new File("docs/modules/html/modules/models/modules/html/modules/gen/apidocs"), false, null, null);	

		copy(new File("../html/model/bootstrap/target/apidocs"), new File("docs/modules/html/modules/models/modules/bootstrap/modules/model/apidocs"), false, null, null);	
		copy(new File("../html/model/bootstrap.gen/target/apidocs"), new File("docs/modules/html/modules/models/modules/bootstrap/modules/gen/apidocs"), false, null, null);	

		copy(new File("../html/model/app/target/apidocs"), new File("docs/modules/html/modules/models/modules/app/modules/model/apidocs"), false, null, null);	
		copy(new File("../html/model/app.gen/target/apidocs"), new File("docs/modules/html/modules/models/modules/app/modules/gen/apidocs"), false, null, null);	

		copy(new File("../engineering/model/target/apidocs"), new File("docs/modules/engineering/modules/model/apidocs"), false, null, null);	
		copy(new File("../engineering/gen/target/apidocs"), new File("docs/modules/engineering/modules/gen/apidocs"), false, null, null);	
		
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
			assertThat(diagnostic.getSeverity()).isNotEqualTo(org.eclipse.emf.common.util.Diagnostic.ERROR);
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
				assertThat(callDiagnostic.getStatus()).isEqualTo(Status.SUCCESS);
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
							&& !path.endsWith("/all-issues.html")
							&& !path.endsWith("/issues.html")
							&& !path.endsWith("-load-specification.html")
							&& !path.endsWith("-all-operations.html")
							&& !path.endsWith("-all-attributes.html")
							&& !path.endsWith("-all-references.html")
							&& !path.endsWith("-all-supertypes.html")) {

						try {
							Document document = Jsoup.parse(file, "UTF-8");
							Elements contentPanelQuery = document.select("body > div > div.row.nsd-app-content-row > div.col.nsd-app-content-panel"); 
							if (contentPanelQuery.size() == 1) {
								Element contentPanel = contentPanelQuery.get(0);
								Elements breadcrumbQuery = contentPanel.select("div > div.row.nsd-app-content-panel-breadcrumb-row > div > nav > ol > li");
								Elements titleQuery = contentPanel.select("div > div.row.nsd-app-content-panel-title-and-items-row > div.col-auto > h1");
								Elements contentQuery = contentPanel.select("div > div.row.nsd-app-content-panel-content-row");
								if (contentQuery.size() == 1) {
									String contentText = contentQuery.get(0).text();
									if (!org.nasdanika.common.Util.isBlank(contentText)) {
										JSONObject searchDocument = new JSONObject();
										searchDocument.put("content", contentText);
										if (titleQuery.size() == 1) {
											searchDocument.put("title", titleQuery.get(0).text());
										} else {
											searchDocument.put("title", document.title());
										}
										if (breadcrumbQuery.size() > 0) {
											searchDocument.put("path", String.join("/", breadcrumbQuery.stream().map(e -> e.text()).collect(Collectors.toList())));
										}
										searchDocuments.put(path, searchDocument);
									}								
								}
							}
						} catch (IOException e) {
							throw new NasdanikaException(e);
						}
					}
				}
			}
		};
		walk(null, listener, docsDir.listFiles());
		wsg.write();	

		try (FileWriter writer = new FileWriter(new File(docsDir, "search-documents.js"))) {
			writer.write("var searchDocuments = " + searchDocuments.toString(4));
		}
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
		return resourceSet;
	}
	
	@Test
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
		
		generateActionModel(name, context, progressMonitor);
		System.out.println("\tGenerated action model in " + (System.currentTimeMillis() - start) + " milliseconds");
		start = System.currentTimeMillis();
		
		generateResourceModel(name, context, progressMonitor);
		System.out.println("\tGenerated resource model in " + (System.currentTimeMillis() - start) + " milliseconds");
		start = System.currentTimeMillis();
		
		generateContainer(name, context, progressMonitor);
		System.out.println("\tGenerated site in " + (System.currentTimeMillis() - start) + " milliseconds");
	}
	
}
