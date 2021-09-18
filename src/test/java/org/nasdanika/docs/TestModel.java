//package org.nasdanika.docs;
//
//import java.io.File;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.Date;
//import java.util.UUID;
//
//import org.eclipse.emf.common.util.URI;
//import org.junit.Test;
//import org.nasdanika.common.Command;
//import org.nasdanika.common.CommandFactory;
//import org.nasdanika.common.Context;
//import org.nasdanika.common.Diagnostic;
//import org.nasdanika.common.DiagnosticException;
//import org.nasdanika.common.DiagramGenerator;
//import org.nasdanika.common.MarkdownHelper;
//import org.nasdanika.common.MutableContext;
//import org.nasdanika.common.NullProgressMonitor;
//import org.nasdanika.common.PrintStreamProgressMonitor;
//import org.nasdanika.common.ProgressMonitor;
//import org.nasdanika.common.Status;
//import org.nasdanika.common.SupplierFactory;
//import org.nasdanika.common.Util;
//import org.nasdanika.common.persistence.ObjectLoader;
//import org.nasdanika.common.persistence.SourceResolver;
//import org.nasdanika.common.persistence.SourceResolver.Link;
//import org.nasdanika.emf.persistence.EObjectLoader;
//import org.nasdanika.engineering.gen.GenerateSiteConsumerFactory;
//import org.nasdanika.engineering.util.EngineeringYamlLoadingExecutionParticipant;
//import org.nasdanika.html.app.Action;
//import org.nasdanika.html.app.factories.BootstrapContainerApplicationSupplierFactory;
//import org.nasdanika.html.app.factories.ComposedLoader;
//import org.nasdanika.html.emf.SimpleEObjectViewAction;
//import org.nasdanika.html.model.app.AppPackage;
//
///**
// * Tests of descriptor view parts and wizards.
// * @author Pavel
// *
// */
//public class TestModel {
//	
//	private static final String NASDANIKA_YAML_PATH = "nasdanika.github.io/target/test-classes/nasdanika.yml";
//	private URI modelURI = URI.createURI(getClass().getResource("/nasdanika.yml").toString());
//
//	@Test
//	public void testGenerateDocsSite() throws Exception {
//		ObjectLoader loader = new EObjectLoader(new ComposedLoader(), null, AppPackage.eINSTANCE);
//		
//		ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
//		String resourceName = "org/nasdanika/html/app/templates/cerulean/dark-fluid.yml";
//		BootstrapContainerApplicationSupplierFactory applicationSupplierFactory = (BootstrapContainerApplicationSupplierFactory) loader.loadYaml(getClass().getClassLoader().getResource(resourceName), progressMonitor);
//		
//		
//		GenerateSiteConsumerFactory consumerFactory = new GenerateSiteConsumerFactory(
//				Collections.singleton(modelURI), 
//				applicationSupplierFactory, 
//				new File("docs"));
//		
//		Object actionFactory = loader.loadYaml(getClass().getResource("/site.yml"), progressMonitor);
//		SupplierFactory<Action> asf = Util.<Action>asSupplierFactory(actionFactory);		
//		
//		CommandFactory commandFactory = asf.then(consumerFactory); 
//		MutableContext context = Context.EMPTY_CONTEXT.fork();
//		context.put(Context.BASE_URI_PROPERTY, "random://" + UUID.randomUUID() + "/" + UUID.randomUUID() + "/");
//		context.put(SimpleEObjectViewAction.DOC_URI, "https://docs.nasdanika.org/engineering/engineering/");
//		context.register(Date.class, new Date());
//
//		URI uri = URI.createFileURI(new File(".").getCanonicalPath());
//		SourceResolver sourceResolver = marker -> {
//			if (marker != null && !Util.isBlank(marker.getLocation())) { 
//				try {
//					File locationFile = new File(new java.net.URI(marker.getLocation()));
//					URI locationURI = URI.createFileURI(locationFile.getCanonicalPath());
//					URI relativeLocationURI = locationURI.deresolve(uri, true, true, true); 
//					String relativeLocationString = relativeLocationURI.toString();
//					return new Link() {
//	
//						@Override
//						public String getLocation() {
//							if (NASDANIKA_YAML_PATH.equals(relativeLocationString)) {
//								return "https://github.com/Nasdanika/nasdanika.github.io/blob/main/src/test/resources/nasdanika.yml#L" + marker.getLine();
//							}
//							int idx = relativeLocationString.indexOf("/");
//							if (idx > 0) {
//								String repository = relativeLocationString.substring(0, idx);
//								String branch = "engineering".equals(repository) ? "main" : "develop"; // Hardcoded - bad, use jGit to figure out the branch.
//								return "https://github.com/Nasdanika/" + repository + "/blob/" + branch + relativeLocationString.substring(idx) + "#L" + marker.getLine();
//							}
//							return marker.getLocation();
//						}
//						
//						@Override
//						public String getText() {	
//							String path = relativeLocationString;
//							if (NASDANIKA_YAML_PATH.equals(path)) {
//								path = "src/test/resources/nasdanika.yml";
//							}
//							
//							return path + " " + marker.getLine() + ":" + marker.getColumn();
//						}
//						
//					};
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//			
//			return null;
//		};
//		
//		context.register(SourceResolver.class, sourceResolver);
//		
//		try {
//			Diagnostic diagnostic = Util.call(commandFactory.create(context), progressMonitor);
//			if (diagnostic.getStatus() == Status.WARNING || diagnostic.getStatus() == Status.ERROR) {
//				System.err.println("***********************");
//				System.err.println("*      Diagnostic     *");
//				System.err.println("***********************");
//				diagnostic.dump(System.err, 4, Status.ERROR, Status.WARNING);
//			}
//		} catch (DiagnosticException e) {
//			System.err.println("******************************");
//			System.err.println("*      Diagnostic failed     *");
//			System.err.println("******************************");
//			e.getDiagnostic().dump(System.err, 4, Status.FAIL);
//			throw e;
//		}
//	}
//
//	/**
//	 * Loading model without rendering to measure loading time.
//	 * @throws Exception
//	 */
//	@Test
//	public void testLoadModel() throws Exception {
//		long start = System.currentTimeMillis();
//		// Outputs to console, send to file if desired.
//		ProgressMonitor progressMonitor = new NullProgressMonitor();
//		
//		class TestCommand extends EngineeringYamlLoadingExecutionParticipant implements Command {
//
//			public TestCommand(Context context) {
//				super(context);
//			}
//
//			@Override
//			protected Collection<URI> getResources() {
//				return Collections.singleton(modelURI);
//			}
//
//			@Override
//			public void execute(ProgressMonitor progressMonitor) throws Exception {
////				Organization org = (Organization) roots.iterator().next();
//				System.out.println("Loaded in " + (System.currentTimeMillis() - start) + " milliseconds.");
//			}
//			
//		};
//		
//		// Diagnosing loaded resources. 
//		try {
//			MutableContext context = Context.EMPTY_CONTEXT.fork();
//			// Do not render markdown
//			context.register(MarkdownHelper.class, new MarkdownHelper() {
//				
//				@Override
//				public String markdownToHtml(String markdown) {
//					return "";
//				}
//				
//			});
//			
//			// Do not generate diagrams
//			context.register(DiagramGenerator.class, new DiagramGenerator() {
//
//				@Override
//				public String generateDiagram(String spec, Dialect dialect) throws Exception {
//					return "";
//				}
//				
//			});
//			
//			org.nasdanika.common.Diagnostic diagnostic = Util.call(new TestCommand(context), progressMonitor);
//			if (diagnostic.getStatus() == Status.WARNING || diagnostic.getStatus() == Status.ERROR) {
//				System.err.println("***********************");
//				System.err.println("*      Diagnostic     *");
//				System.err.println("***********************");
//				diagnostic.dump(System.err, 4, Status.ERROR, Status.WARNING);
//			}
//		} catch (DiagnosticException e) {
//			System.err.println("******************************");
//			System.err.println("*      Diagnostic failed     *");
//			System.err.println("******************************");
//			e.getDiagnostic().dump(System.err, 4, Status.FAIL);
//			throw e;
//		}
//		
//	}
//	
//
//}
