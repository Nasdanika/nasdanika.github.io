package org.nasdanika.docs.engineering;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.codec.binary.Hex;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.GenPackage;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.TreeIterator;
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
import org.junit.Test;
import org.nasdanika.common.Context;
import org.nasdanika.common.DefaultConverter;
import org.nasdanika.common.Diagnostic;
import org.nasdanika.common.DiagramGenerator;
import org.nasdanika.common.MutableContext;
import org.nasdanika.common.NasdanikaException;
import org.nasdanika.common.NullProgressMonitor;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.common.Status;
import org.nasdanika.common.resources.BinaryEntityContainer;
import org.nasdanika.common.resources.FileSystemContainer;
import org.nasdanika.diagram.DiagramPackage;
import org.nasdanika.emf.EObjectAdaptable;
import org.nasdanika.emf.EmfUtil;
import org.nasdanika.emf.persistence.FeatureCacheAdapter;
import org.nasdanika.engineering.EngineeringPackage;
import org.nasdanika.engineering.gen.EngineeringActionProviderAdapterFactory;
import org.nasdanika.exec.ExecPackage;
import org.nasdanika.exec.content.ContentPackage;
import org.nasdanika.exec.resources.Container;
import org.nasdanika.exec.resources.ReconcileAction;
import org.nasdanika.exec.resources.ResourcesFactory;
import org.nasdanika.exec.resources.ResourcesPackage;
import org.nasdanika.flow.FlowPackage;
import org.nasdanika.html.ecore.EcoreActionSupplierAdapterFactory;
import org.nasdanika.html.ecore.GenModelResourceSet;
import org.nasdanika.html.emf.EObjectActionResolver;
import org.nasdanika.html.model.app.Action;
import org.nasdanika.html.model.app.AppPackage;
import org.nasdanika.html.model.app.gen.AppAdapterFactory;
import org.nasdanika.html.model.app.gen.Util;
import org.nasdanika.html.model.app.util.ActionProvider;
import org.nasdanika.html.model.app.util.ActionSupplier;
import org.nasdanika.html.model.bootstrap.BootstrapPackage;
import org.nasdanika.html.model.html.HtmlPackage;
import org.nasdanika.ncore.Marker;
import org.nasdanika.ncore.NcorePackage;
import org.nasdanika.ncore.util.NcoreResourceSet;

/**
 * Tests of agile flows.
 * @author Pavel
 *
 */
public class TestNasdanikaDocEngineeringGen extends TestBase {
	
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
	protected void generateEngineeringModel(String name, ProgressMonitor progressMonitor) throws Exception {	
		load(
				"engineering/" + name + ".yml", 
				obj -> {
					EObject copy = EcoreUtil.copy(obj);
					ResourceSet resourceSet = new NcoreResourceSet();
					resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(org.eclipse.emf.ecore.resource.Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
					
					org.eclipse.emf.ecore.resource.Resource instanceModelResource = resourceSet.createResource(URI.createURI(name + ".xml").resolve(ENGINEERING_MODELS_URI));
					instanceModelResource.getContents().add(copy);
					
					org.eclipse.emf.common.util.Diagnostic copyDiagnostic = org.nasdanika.emf.EmfUtil.resolveClearCacheAndDiagnose(resourceSet, Context.EMPTY_CONTEXT);
					int severity = copyDiagnostic.getSeverity();
					if (severity != org.eclipse.emf.common.util.Diagnostic.OK) {
						EmfUtil.dumpDiagnostic(copyDiagnostic, 2, System.err);
					}
					assertThat(severity).isEqualTo(org.eclipse.emf.common.util.Diagnostic.OK);
										
					try {
						// Marker - TODO - use jGit because of a federated nature of the engineering model 
						Map<String,String> originMap = new HashMap<>();
						originMap.put(new File(".").getCanonicalFile().toURI().toString(), "https://github.com/Nasdanika/nasdanika.github.io/blob/main/");
						originMap.put(new File("../core").getCanonicalFile().toURI().toString(), "https://github.com/Nasdanika/core/blob/master/");
						originMap.put(new File("../html").getCanonicalFile().toURI().toString(), "https://github.com/Nasdanika/html/blob/master/");
						originMap.put(new File("../engineering").getCanonicalFile().toURI().toString(), "https://github.com/Nasdanika/engineering/blob/main/");
						originMap.put(new File("../togaf").getCanonicalFile().toURI().toString(), "https://github.com/Nasdanika/togaf/blob/main/");
						TreeIterator<EObject> cit = copy.eAllContents(); 
						while (cit.hasNext()) {
							EObject next = cit.next(); 
							if (next instanceof Marker) {
								Marker marker = (Marker) next;
								String location = marker.getLocation();
								for (Map.Entry<String,String> originEntry: originMap.entrySet()) {
									if (location != null && location.startsWith(originEntry.getKey())) {
										marker.setLocation(location.substring(originEntry.getKey().length()));
										marker.setOrigin(originEntry.getValue() + marker.getLocation());
										if (marker.getLine() > 0) {
											marker.setOrigin(marker.getOrigin() + "#L" + marker.getLine());
										}
									}
								}
							}
						}
						
						instanceModelResource.save(null);
					} catch (IOException ioe) {
						throw new NasdanikaException(ioe);
					}
				},
				diagnostic -> {
					if (diagnostic.getStatus() == Status.FAIL || diagnostic.getStatus() == Status.ERROR) {
						System.err.println("***********************");
						System.err.println("*      Diagnostic     *");
						System.err.println("***********************");
						diagnostic.dump(System.err, 4, Status.FAIL, Status.ERROR);
					}
					assertThat(diagnostic.getStatus()).isEqualTo(Status.SUCCESS);
				},
				progressMonitor);		
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
		
	public void generateEcoreActionModel(ProgressMonitor progressMonitor) throws Exception {
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
		
		MutableContext context = Context.EMPTY_CONTEXT.fork();
		
		DiagramGenerator diagramGenerator = createDiagramGenerator(progressMonitor);
		context.register(DiagramGenerator.class, diagramGenerator);//DiagramGenerator.createClient(new URL("http://localhost:8090/spring-exec/api/v1/exec/diagram/")));
		
		ecoreModelsResourceSet.getAdapterFactories().add(new EcoreActionSupplierAdapterFactory(context, getEPackagePath));
		
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
						actionModelResource.getContents().add(EObjectAdaptable.adaptTo(ecorePackage, ActionSupplier.class).execute(progressMonitor));
					}
				}
			}
	
			actionModelResource.save(null);
		}		
	}
		
	/**
	 * Loads instance model from previously generated XMI, diagnoses, generates action model.
	 * @throws Exception
	 */
	public void generateActionModel(String name, ProgressMonitor progressMonitor) throws Exception {
		ResourceSet instanceModelsResourceSet = createResourceSet();
		Resource instanceModelResource = instanceModelsResourceSet.getResource(URI.createURI(name + ".xml").resolve(ENGINEERING_MODELS_URI), true);

		org.eclipse.emf.common.util.Diagnostic instanceDiagnostic = org.nasdanika.emf.EmfUtil.resolveClearCacheAndDiagnose(instanceModelsResourceSet, Context.EMPTY_CONTEXT);
		int severity = instanceDiagnostic.getSeverity();
		if (severity != org.eclipse.emf.common.util.Diagnostic.OK) {
			EmfUtil.dumpDiagnostic(instanceDiagnostic, 2, System.err);
		}
		assertThat(severity).isEqualTo(org.eclipse.emf.common.util.Diagnostic.OK);
		
		instanceModelsResourceSet.getAdapterFactories().add(new EngineeringActionProviderAdapterFactory(Context.EMPTY_CONTEXT) {
			
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
		
		ResourceSet actionModelsResourceSet = createResourceSet();
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
	
	/**
	 * Generates a resource model from an action model.
	 * @throws Exception
	 */
	public void generateResourceModel(String name, ProgressMonitor progressMonitor) throws Exception {
		Consumer<Diagnostic> diagnosticConsumer = diagnostic -> {
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
				Context.EMPTY_CONTEXT,
				progressMonitor);
		
		modelResource.save(null);
	}
	
	/**
	 * Generates files from the previously generated resource model.
	 * @throws Exception
	 */
	public void generateContainer(String name, ProgressMonitor progressMonitor) throws Exception {
		ResourceSet resourceSet = createResourceSet();
		
		resourceSet.getAdapterFactories().add(new AppAdapterFactory());
				
		Resource containerResource = resourceSet.getResource(URI.createURI(name + ".xml").resolve(RESOURCE_MODELS_URI), true);
	
		File siteDir = new File("target/model-doc/site");
		BinaryEntityContainer container = new FileSystemContainer(siteDir);
		for (EObject eObject : containerResource.getContents()) {
			Diagnostician diagnostician = new Diagnostician();
			org.eclipse.emf.common.util.Diagnostic diagnostic = diagnostician.validate(eObject);
			assertThat(diagnostic.getSeverity()).isNotEqualTo(org.eclipse.emf.common.util.Diagnostic.ERROR);
			generate(eObject, container, Context.EMPTY_CONTEXT, progressMonitor);
		}
		
		// Cleanup docs, keep CNAME, favicon.ico, and images directory. Copy from target/model-doc/site/nasdanika
		Predicate<String> cleanPredicate = path -> {
			return !"CNAME".equals(path) && !"favicon.ico".equals(path) && !path.startsWith("images/");
		};

		copy(new File(siteDir, "nasdanika"), new File("docs"), true, cleanPredicate, null);

	}
	
	protected ResourceSet createResourceSet() {
		// Load model from XMI
		ResourceSet resourceSet = new NcoreResourceSet();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
	
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
		
		long start = System.currentTimeMillis();
		generateEcoreActionModel(progressMonitor);
		System.out.println("\tGenerated ecore action model in " + (System.currentTimeMillis() - start) + " milliseconds");
		start = System.currentTimeMillis();
		
		generateSite("nasdanika", progressMonitor);
		
		long cacheMisses = FeatureCacheAdapter.getMisses();
		long cacheCalls = FeatureCacheAdapter.getCalls();
		long cacheEfficiency = 100*(cacheCalls - cacheMisses)/cacheCalls;
		System.out.println("Feature cache - calls: " + cacheCalls + ", misses: " + cacheMisses + ", efficiency: " + cacheEfficiency + "%, compute time: " + TimeUnit.NANOSECONDS.toMillis(FeatureCacheAdapter.getComputeTime()) + " milliseconds.");
	}
	
	private void generateSite(String name, ProgressMonitor progressMonitor) throws Exception {
		System.out.println("Generating site: " + name);
		
		long start = System.currentTimeMillis();
		generateEngineeringModel(name, progressMonitor);
		System.out.println("\tGenerated instance model in " + (System.currentTimeMillis() - start) + " milliseconds");
		start = System.currentTimeMillis();
		
		generateActionModel(name, progressMonitor);
		System.out.println("\tGenerated action model in " + (System.currentTimeMillis() - start) + " milliseconds");
		start = System.currentTimeMillis();
		
		generateResourceModel(name, progressMonitor);
		System.out.println("\tGenerated resource model in " + (System.currentTimeMillis() - start) + " milliseconds");
		start = System.currentTimeMillis();
		
		generateContainer(name, progressMonitor);
		System.out.println("\tGenerated site in " + (System.currentTimeMillis() - start) + " milliseconds");
	}
	
}
