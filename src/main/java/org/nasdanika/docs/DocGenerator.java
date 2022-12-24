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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.codec.binary.Hex;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.GenPackage;
import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
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
import org.nasdanika.common.SupplierFactory;
import org.nasdanika.emf.EObjectAdaptable;
import org.nasdanika.emf.persistence.EObjectLoader;
import org.nasdanika.emf.persistence.FeatureCacheAdapter;
import org.nasdanika.exec.ExecPackage;
import org.nasdanika.exec.content.ContentFactory;
import org.nasdanika.exec.content.ContentPackage;
import org.nasdanika.exec.resources.Container;
import org.nasdanika.exec.resources.ReconcileAction;
import org.nasdanika.exec.resources.ResourcesFactory;
import org.nasdanika.exec.resources.ResourcesPackage;
import org.nasdanika.html.ecore.EcoreActionSupplier;
import org.nasdanika.html.ecore.EcoreActionSupplierAdapterFactory;
import org.nasdanika.html.ecore.GenModelResourceSet;
import org.nasdanika.html.jstree.JsTreeFactory;
import org.nasdanika.html.jstree.JsTreeNode;
import org.nasdanika.html.model.app.Action;
import org.nasdanika.html.model.app.AppFactory;
import org.nasdanika.html.model.app.AppPackage;
import org.nasdanika.html.model.app.Label;
import org.nasdanika.html.model.app.Link;
import org.nasdanika.html.model.app.gen.ActionContentProvider;
import org.nasdanika.html.model.app.gen.AppAdapterFactory;
import org.nasdanika.html.model.app.gen.LabelJsTreeNodeSupplierFactoryAdapter;
import org.nasdanika.html.model.app.gen.LinkJsTreeNodeSupplierFactoryAdapter;
import org.nasdanika.html.model.app.gen.NavigationPanelConsumerFactoryAdapter;
import org.nasdanika.html.model.app.gen.PageContentProvider;
import org.nasdanika.html.model.app.gen.Util;
import org.nasdanika.html.model.app.util.AppObjectLoaderSupplier;
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
	private static final File ACTION_MODELS_DIR = new File(GENERATED_MODELS_BASE_DIR, "actions");
	private static final File RESOURCE_MODELS_DIR = new File(GENERATED_MODELS_BASE_DIR, "resources");
	
	private static final URI RESOURCE_MODELS_URI = URI.createFileURI(RESOURCE_MODELS_DIR.getAbsolutePath() + "/");	
	
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
	
	protected EObject loadObject(
			String resource, 
			Consumer<org.nasdanika.common.Diagnostic> diagnosticConsumer,
			Context context,
			ProgressMonitor progressMonitor) throws Exception {
		
		URI resourceURI = URI.createFileURI(new File(resource).getAbsolutePath());
				
		// Diagnosing loaded resources. 
		try {
			return Objects.requireNonNull(org.nasdanika.common.Util.call(new AppObjectLoaderSupplier(resourceURI, context), progressMonitor, diagnosticConsumer), "Loaded null from " + resource);
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
			if (diagnostic.getStatus() != Status.SUCCESS) {
				throw new DiagnosticException(diagnostic);
			};
		};
		
		Context modelContext = Context.singleton("model-name", name);
		String actionsResource = "model/nasdanika.yml";
		Action root = (Action) Objects.requireNonNull(loadObject(actionsResource, diagnosticConsumer, modelContext, progressMonitor), "Loaded null from " + actionsResource);
		root.eResource().getResourceSet().getAdapterFactories().add(new AppAdapterFactory());
		
		Container container = ResourcesFactory.eINSTANCE.createContainer();
		container.setName(name);
		container.setReconcileAction(ReconcileAction.OVERWRITE);
		
		ResourceSet resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
		Resource modelResource = resourceSet.createResource(URI.createURI(name + ".xml").resolve(RESOURCE_MODELS_URI));
		modelResource.getContents().add(container);
		
		String pageTemplateResource = "model/page-template.yml";
		org.nasdanika.html.model.bootstrap.Page pageTemplate = (org.nasdanika.html.model.bootstrap.Page) Objects.requireNonNull(loadObject(pageTemplateResource, diagnosticConsumer, modelContext, progressMonitor), "Loaded null from " + pageTemplateResource);
		
		File contentDir = new File(RESOURCE_MODELS_DIR, "content");
		contentDir.mkdirs();
		// Generating content file from action content 
		ActionContentProvider.Factory actionContentProviderFactory = (contentProviderContext) -> (action, uriResolver, pMonitor) -> {
			
			@SuppressWarnings("unchecked")
			java.util.function.Function<Context, String> siteMapTreeScriptComputer = ctx -> {
				// TODO - actions from action prototype, e.g. Ecore doc actions, to the tree.
				
				JsTreeFactory jsTreeFactory = context.get(JsTreeFactory.class, JsTreeFactory.INSTANCE);
				Map<Action, JsTreeNode> actionMap = new HashMap<>();
				TreeIterator<EObject> ait = root.eAllContents();
				while (ait.hasNext()) {
					EObject next = ait.next();
					if (next instanceof Action) {
						Action treeAction = (Action) next;

						URI bURI = uriResolver.apply(action, (URI) null);
						URI tURI = uriResolver.apply(treeAction, bURI);
						
						Label label = tURI == null ? AppFactory.eINSTANCE.createLabel() : AppFactory.eINSTANCE.createLink();
						String treeActionText = treeAction.getText();
						int maxLength = 50;
						label.setText(treeActionText.length() > maxLength ? treeActionText.substring(0, maxLength) + "..." : treeActionText);
						label.setIcon(treeAction.getIcon());
						
						LabelJsTreeNodeSupplierFactoryAdapter<?> adapter;
						if (label instanceof Link) {
							((Link) label).setLocation(tURI.toString());
							adapter = new LinkJsTreeNodeSupplierFactoryAdapter<Link>((Link) label);
						} else {
							adapter = new LabelJsTreeNodeSupplierFactoryAdapter<Label>(label);							
						}
						
						try {
							JsTreeNode jsTreeNode = adapter.create(ctx).execute(progressMonitor);
							jsTreeNode.attribute(Util.DATA_NSD_ACTION_UUID_ATTRIBUTE, treeAction.getUuid());
							actionMap.put(treeAction, jsTreeNode);
						} catch (Exception e) {
							throw new NasdanikaException(e);
						}
					}
				}
				
				Map<Action, JsTreeNode> roots = new HashMap<>(actionMap);
				
				Map<EObject,Map<String,List<JsTreeNode>>> refMap = new HashMap<>();
				for (EObject eObj: new ArrayList<>(actionMap.keySet())) {
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
				
				for (Entry<Action, JsTreeNode> ne: actionMap.entrySet()) {
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
		// TODO - NASDAF
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
		FileSystemContainer container = new FileSystemContainer(siteDir);
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
				
		try (InputStream in = new FileInputStream("model/search.js")) {
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
		
		org.nasdanika.persistence.ObjectLoader objectLoader = new EObjectLoader(null, null, resourceSet);
		
		org.nasdanika.persistence.ObjectLoaderResourceFactory yamlResourceFactory = new org.nasdanika.persistence.ObjectLoaderResourceFactory() {
			
			@Override
			protected org.nasdanika.persistence.ObjectLoader getObjectLoader(Resource resource) {
				return objectLoader;
			}
			
		};
		extensionToFactoryMap.put("yml", yamlResourceFactory);
	
		resourceSet.getPackageRegistry().put(NcorePackage.eNS_URI, NcorePackage.eINSTANCE);
		resourceSet.getPackageRegistry().put(ExecPackage.eNS_URI, ExecPackage.eINSTANCE);
		resourceSet.getPackageRegistry().put(ContentPackage.eNS_URI, ContentPackage.eINSTANCE);
		resourceSet.getPackageRegistry().put(ResourcesPackage.eNS_URI, ResourcesPackage.eINSTANCE);
		resourceSet.getPackageRegistry().put(HtmlPackage.eNS_URI, HtmlPackage.eINSTANCE);
		resourceSet.getPackageRegistry().put(BootstrapPackage.eNS_URI, BootstrapPackage.eINSTANCE);
		resourceSet.getPackageRegistry().put(AppPackage.eNS_URI, AppPackage.eINSTANCE);
		
		// TODO - NASDAF
		
//		resourceSet.getAdapterFactories().add(new AppAdapterFactory())				
		return resourceSet;
	}
	
	public void generate() throws Exception {
		delete(ACTION_MODELS_DIR);
		delete(RESOURCE_MODELS_DIR);
		
		ACTION_MODELS_DIR.mkdirs();
		RESOURCE_MODELS_DIR.mkdirs();

		ProgressMonitor progressMonitor = new NullProgressMonitor(); // PrintStreamProgressMonitor();		

		Function<String, String> nasdanikaResolver = org.nasdanika.common.Util.createNasdanikaJavadocResolver(new File(".."), progressMonitor);
		
		MutableContext context = Context.EMPTY_CONTEXT.fork();
		context.put("javadoc", org.nasdanika.common.Util.createJavadocPropertyComputer(nasdanikaResolver));
		
		DiagramGenerator diagramGenerator = createDiagramGenerator(progressMonitor);
		context.register(DiagramGenerator.class, diagramGenerator);//DiagramGenerator.createClient(new URL("http://localhost:8090/spring-exec/api/v1/exec/diagram/")));
		
		long start = System.currentTimeMillis();
//		generateEcoreActionModel(context, progressMonitor);
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
		generateResourceModel(name, context, progressMonitor);
		System.out.println("\tGenerated resource model in " + (System.currentTimeMillis() - start) + " milliseconds");
		start = System.currentTimeMillis();
		
		generateContainer(name, context, progressMonitor);
		System.out.println("\tGenerated site in " + (System.currentTimeMillis() - start) + " milliseconds");
	}

}
