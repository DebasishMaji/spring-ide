/*
 * Copyright 2002-2007 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ide.eclipse.webflow.ui.graph;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.parts.ScrollableThumbnail;
import org.eclipse.draw2d.parts.Thumbnail;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.RootEditPart;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.tools.ConnectionCreationTool;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.DirectEditAction;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.gef.ui.parts.ContentOutlinePage;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.gef.ui.parts.TreeViewer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.xml.core.internal.cleanup.CleanupProcessorXML;
import org.eclipse.wst.xml.core.internal.document.DOMModelImpl;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.eclipse.wst.xml.core.internal.provisional.format.FormatProcessorXML;
import org.springframework.ide.eclipse.webflow.core.internal.model.WebflowConfig;
import org.springframework.ide.eclipse.webflow.core.internal.model.WebflowState;
import org.springframework.ide.eclipse.webflow.core.model.IWebflowConfig;
import org.springframework.ide.eclipse.webflow.core.model.IWebflowState;
import org.springframework.ide.eclipse.webflow.ui.editor.Activator;
import org.springframework.ide.eclipse.webflow.ui.graph.actions.EditPropertiesAction;
import org.springframework.ide.eclipse.webflow.ui.graph.actions.ExportAction;
import org.springframework.ide.eclipse.webflow.ui.graph.actions.OpenBeansConfigAction;
import org.springframework.ide.eclipse.webflow.ui.graph.actions.OpenBeansGraphAction;
import org.springframework.ide.eclipse.webflow.ui.graph.actions.OpenConfigFileAction;
import org.springframework.ide.eclipse.webflow.ui.graph.actions.SetAsStartStateAction;
import org.springframework.ide.eclipse.webflow.ui.graph.actions.WebflowContextMenuProvider;
import org.springframework.ide.eclipse.webflow.ui.graph.parts.StatePartFactory;
import org.springframework.ide.eclipse.webflow.ui.graph.parts.StateTreeEditPartFactory;
import org.w3c.dom.Node;

/**
 * 
 */
@SuppressWarnings("restriction")
public class WebflowEditor extends GraphicalEditorWithFlyoutPalette implements
		ITabbedPropertySheetPageContributor {

	/**
	 * 
	 */
	private class OutlinePage extends ContentOutlinePage implements IAdaptable {

		/**
		 * 
		 */
		static final int ID_OUTLINE = 0;

		/**
		 * 
		 */
		static final int ID_OVERVIEW = 1;

		/**
		 * 
		 */
		private DisposeListener disposeListener;

		/**
		 * 
		 */
		private Control outline;

		/**
		 * 
		 */
		private Canvas overview;

		/**
		 * 
		 */
		private PageBook pageBook;

		/**
		 * 
		 */
		private IAction showOutlineAction, showOverviewAction;

		/**
		 * 
		 */
		private Thumbnail thumbnail;

		/**
		 * @param viewer
		 */
		public OutlinePage(EditPartViewer viewer) {
			super(viewer);
		}

		/**
		 * 
		 */
		protected void configureOutlineViewer() {
			getViewer().setEditDomain(getEditDomain());
			getViewer().setEditPartFactory(new StateTreeEditPartFactory());
			ContextMenuProvider provider = new WebflowContextMenuProvider(
					getViewer(), getActionRegistry());
			getViewer().setContextMenu(provider);
			getSite()
					.registerContextMenu(
							"org.springframework.ide.eclipse.webflow.ui.graph.contextmenu", //$NON-NLS-1$
							provider, getSite().getSelectionProvider());
			getViewer().setKeyHandler(getCommonKeyHandler());
			IToolBarManager tbm = getSite().getActionBars().getToolBarManager();
			showOutlineAction = new Action() {

				public void run() {
					showPage(ID_OUTLINE);
				}
			};
			showOutlineAction
					.setImageDescriptor(WebflowImages.DESC_OBJS_OUTLINE); //$NON-NLS-1$
			showOutlineAction.setToolTipText("Show tree outline");
			tbm.add(showOutlineAction);
			showOverviewAction = new Action() {

				public void run() {
					showPage(ID_OVERVIEW);
				}
			};
			showOverviewAction
					.setImageDescriptor(WebflowImages.DESC_OBJS_OVERVIEW); //$NON-NLS-1$
			showOverviewAction.setToolTipText("Show graphical outline");
			tbm.add(showOverviewAction);
			showPage(ID_OUTLINE);
		}

		/**
		 * @param parent
		 */
		public void createControl(Composite parent) {
			pageBook = new PageBook(parent, SWT.NONE);
			outline = getViewer().createControl(pageBook);
			overview = new Canvas(pageBook, SWT.NONE);
			pageBook.showPage(outline);
			configureOutlineViewer();
			hookOutlineViewer();
			initializeOutlineViewer();
		}

		/**
		 * 
		 */
		public void dispose() {
			unhookOutlineViewer();
			if (thumbnail != null) {
				thumbnail.deactivate();
				thumbnail = null;
			}
			super.dispose();
			WebflowEditor.this.outlinePage = null;
		}

		/**
		 * @param type
		 * @return
		 */
		public Object getAdapter(Class type) {
			if (type == ZoomManager.class)
				return getGraphicalViewer().getProperty(
						ZoomManager.class.toString());
			return null;
		}

		/**
		 * @return
		 */
		public Control getControl() {
			return pageBook;
		}

		/**
		 * 
		 */
		protected void hookOutlineViewer() {
			getSelectionSynchronizer().addViewer(getViewer());
		}

		/**
		 * @param pageSite
		 */
		public void init(IPageSite pageSite) {
			super.init(pageSite);
			ActionRegistry registry = getActionRegistry();
			IActionBars bars = pageSite.getActionBars();
			String id = ActionFactory.UNDO.getId();
			bars.setGlobalActionHandler(id, registry.getAction(id));
			id = ActionFactory.REDO.getId();
			bars.setGlobalActionHandler(id, registry.getAction(id));
			id = ActionFactory.DELETE.getId();
			bars.setGlobalActionHandler(id, registry.getAction(id));
			id = SetAsStartStateAction.STARTSTATE;
			bars.setGlobalActionHandler(id, registry.getAction(id));
			bars.updateActionBars();
		}

		/**
		 * 
		 */
		public void initializeOutlineViewer() {
			setContents(diagram);
		}

		/**
		 * 
		 */
		protected void initializeOverview() {
			LightweightSystem lws = new LightweightSystem(overview);
			RootEditPart rep = getGraphicalViewer().getRootEditPart();
			if (rep instanceof ScalableRootEditPart) {
				ScalableRootEditPart root = (ScalableRootEditPart) rep;
				thumbnail = new ScrollableThumbnail((Viewport) root.getFigure());
				thumbnail.setBorder(new MarginBorder(3));
				thumbnail.setSource(root
						.getLayer(LayerConstants.PRINTABLE_LAYERS));
				lws.setContents(thumbnail);
				disposeListener = new DisposeListener() {

					public void widgetDisposed(DisposeEvent e) {
						if (thumbnail != null) {
							thumbnail.deactivate();
							thumbnail = null;
						}
					}
				};
				getEditor().addDisposeListener(disposeListener);
			}
		}

		/**
		 * @param contents
		 */
		public void setContents(Object contents) {
			getViewer().setContents(contents);
		}

		/**
		 * @param id
		 */
		protected void showPage(int id) {
			if (id == ID_OUTLINE) {
				showOutlineAction.setChecked(true);
				showOverviewAction.setChecked(false);
				pageBook.showPage(outline);
				if (thumbnail != null)
					thumbnail.setVisible(false);
			}
			else if (id == ID_OVERVIEW) {
				if (thumbnail == null)
					initializeOverview();
				showOutlineAction.setChecked(false);
				showOverviewAction.setChecked(true);
				pageBook.showPage(overview);
				thumbnail.setVisible(true);
			}
		}

		/**
		 * 
		 */
		protected void unhookOutlineViewer() {
			getSelectionSynchronizer().removeViewer(getViewer());
			if (disposeListener != null && getEditor() != null
					&& !getEditor().isDisposed())
				getEditor().removeDisposeListener(disposeListener);
		}
	}

	/**
	 * 
	 */
	private class ResourceTracker implements IResourceChangeListener,
			IResourceDeltaVisitor {

		/**
		 * @param event
		 */
		public void resourceChanged(IResourceChangeEvent event) {
			IResourceDelta delta = event.getDelta();
			try {
				if (delta != null)
					delta.accept(this);
			}
			catch (CoreException exception) {
				// What should be done here?
			}
		}

		/**
		 * @param delta
		 * @return
		 */
		public boolean visit(IResourceDelta delta) {
			if (delta == null
					|| !delta.getResource().equals(
							((WebflowEditorInput) getEditorInput()).getFile()))
				return true;

			if (delta.getKind() == IResourceDelta.REMOVED) {
				Display display = getSite().getShell().getDisplay();
				if ((IResourceDelta.MOVED_TO & delta.getFlags()) == 0) { // if

					display.asyncExec(new Runnable() {

						public void run() {
							if (!isDirty())
								closeEditor(false);
						}
					});
				}
				else { // else if it was moved or renamed
					final IFile newFile = ResourcesPlugin.getWorkspace()
							.getRoot().getFile(delta.getMovedToPath());
					display.asyncExec(new Runnable() {

						public void run() {
							IWebflowConfig config = ((WebflowEditorInput) getEditorInput())
									.getConfig();
							config.setResource(newFile);
							List<IWebflowConfig> configs = config.getProject()
									.getConfigs();
							config.getProject().setConfigs(configs);
							superSetInput(new WebflowEditorInput(config));
						}
					});
				}
			}
			else if (delta.getKind() == IResourceDelta.CHANGED) {
				if (!isDirty() || isCurrentlySaving) {
					//final IFile newFile = ResourcesPlugin.getWorkspace()
					//		.getRoot().getFile(delta.getFullPath());
					Display display = getSite().getShell().getDisplay();
					display.asyncExec(new Runnable() {

						public void run() {
							IWebflowConfig config = ((WebflowEditorInput) getEditorInput())
									.getConfig();
							setInput(new WebflowEditorInput(config));
							getCommandStack().flush();
							initializeGraphicalViewer();
							outlinePage.initializeOutlineViewer();
						}
					});
				}
				else if (isDirty()
						&& MessageDialog
								.openQuestion(
										Activator.getDefault().getWorkbench()
												.getActiveWorkbenchWindow()
												.getShell(),
										"File Changed",
										"The file has been changed on the file system. Do you want to load the changes?")) {
					//final IFile newFile = ResourcesPlugin.getWorkspace()
					//		.getRoot().getFile(delta.getFullPath());
					Display display = getSite().getShell().getDisplay();
					display.asyncExec(new Runnable() {

						public void run() {
							IWebflowConfig config = ((WebflowEditorInput) getEditorInput())
									.getConfig();
							setInput(new WebflowEditorInput(config));
							getCommandStack().flush();
							initializeGraphicalViewer();
							outlinePage.initializeOutlineViewer();
						}
					});
				}
			}
			return false;
		}
	}

	/**
	 * 
	 */
	private IWebflowState diagram;

	/**
	 * 
	 */
	private IFile file;

	/**
	 * 
	 */
	private boolean isCurrentlySaving = false;

	/**
	 * 
	 */
	private IStructuredModel model;

	/**
	 * 
	 */
	private OutlinePage outlinePage;

	/**
	 * 
	 */
	private IPartListener partListener = new IPartListener() {

		// If an open, unsaved file was deleted, query the user to either do a
		// "Save As"
		// or close the editor.
		public void partActivated(IWorkbenchPart part) {
			if (part != WebflowEditor.this)
				return;
			if (!((WebflowEditorInput) getEditorInput()).getFile().exists()) {
				Shell shell = getSite().getShell();
				String title = "res";
				String message = "erer";
				String[] buttons = { "Save", "Close" };
				MessageDialog dialog = new MessageDialog(shell, title, null,
						message, MessageDialog.QUESTION, buttons, 0);
				if (dialog.open() == 0) {
					if (!performSaveAs())
						partActivated(part);
				}
				else {
					closeEditor(false);
				}
			}
		}

		public void partBroughtToTop(IWorkbenchPart part) {
		}

		public void partClosed(IWorkbenchPart part) {
		}

		public void partDeactivated(IWorkbenchPart part) {
		}

		public void partOpened(IWorkbenchPart part) {
		}

	};

	/**
	 * 
	 */
	private ResourceTracker resourceListener = new ResourceTracker();

	/**
	 * 
	 */
	private PaletteRoot root;

	/**
	 * 
	 */
	private boolean savePreviouslyNeeded = false;

	/**
	 * 
	 */
	private KeyHandler sharedKeyHandler;

	public static final String EDITOR_ID = "org.springframework.ide.eclipse.webflow.ui.graph.WebflowEditor";

	/**
	 * 
	 */
	public WebflowEditor() {
		DefaultEditDomain defaultEditDomain = new DefaultEditDomain(this);
		defaultEditDomain.setActiveTool(new ConnectionCreationTool());
		setEditDomain(defaultEditDomain);
	}

	/**
	 * @param save
	 */
	protected void closeEditor(boolean save) {
		getSite().getPage().closeEditor(this, save);
	}

	/**
	 * @param event
	 * @see org.eclipse.gef.commands.CommandStackListener#commandStackChanged(java.util.EventObject)
	 */
	public void commandStackChanged(EventObject event) {
		if (isDirty()) {
			if (!savePreviouslyNeeded()) {
				setSavePreviouslyNeeded(true);
				firePropertyChange(IEditorPart.PROP_DIRTY);
			}
		}
		else {
			setSavePreviouslyNeeded(false);
			firePropertyChange(IEditorPart.PROP_DIRTY);
		}
		super.commandStackChanged(event);
	}

	/**
	 * @see org.eclipse.gef.ui.parts.GraphicalEditor#configureGraphicalViewer()
	 */
	protected void configureGraphicalViewer() {
		super.configureGraphicalViewer();
		ScalableRootEditPart root = new ScalableRootEditPart();

		List<String> zoomLevels = new ArrayList<String>();
		zoomLevels.add(ZoomManager.FIT_ALL);
		zoomLevels.add(ZoomManager.FIT_WIDTH);
		zoomLevels.add(ZoomManager.FIT_HEIGHT);
		root.getZoomManager().setZoomLevelContributions(zoomLevels);

		IAction zoomIn = new ZoomInAction(root.getZoomManager());
		IAction zoomOut = new ZoomOutAction(root.getZoomManager());
		getActionRegistry().registerAction(zoomIn);
		getActionRegistry().registerAction(zoomOut);

		getActionRegistry().registerAction(new ExportAction(this));

		getSite().getKeyBindingService().registerAction(zoomIn);
		getSite().getKeyBindingService().registerAction(zoomOut);
		getGraphicalViewer().setRootEditPart(root);
		getGraphicalViewer().setEditPartFactory(new StatePartFactory());
		getGraphicalViewer().setKeyHandler(
				new GraphicalViewerKeyHandler(getGraphicalViewer())
						.setParent(getCommonKeyHandler()));

		ContextMenuProvider provider = new WebflowContextMenuProvider(
				getGraphicalViewer(), getActionRegistry());
		getGraphicalViewer().setContextMenu(provider);
		getSite().registerContextMenu(
				"org.springframework.ide.eclipse.webflow.ui.graph.contextmenu", //$NON-NLS-1$
				provider, getGraphicalViewer());

	}

	/**
	 * @see org.eclipse.gef.ui.parts.GraphicalEditor#createActions()
	 */
	@SuppressWarnings("unchecked")
	protected void createActions() {
		super.createActions();
		ActionRegistry registry = getActionRegistry();
		IAction action;

		action = new DirectEditAction((IWorkbenchPart) this);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new SetAsStartStateAction(this);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new EditPropertiesAction(this);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new ExportAction(this);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new OpenConfigFileAction(this);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new OpenBeansGraphAction(this);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new OpenBeansConfigAction(this);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

	}

	/**
	 * 
	 */
	public void dispose() {
		getSite().getWorkbenchWindow().getPartService().removePartListener(
				partListener);
		partListener = null;
		((WebflowEditorInput) getEditorInput()).getFile().getWorkspace()
				.removeResourceChangeListener(resourceListener);

		if (this.diagram != null) {
			diagram = null;
		}

		if (this.model != null) {
			this.model.releaseFromEdit();
		}

		super.dispose();
	}

	/**
	 * @param monitor
	 * @see org.eclipse.ui.ISaveablePart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void doSave(IProgressMonitor monitor) {
		try {
			this.isCurrentlySaving = true;
			model.aboutToChangeModel();
			// reattach root node from document
			IDOMDocument document = ((DOMModelImpl) model).getDocument();
			Node root = document.getDocumentElement();
			document.replaceChild(diagram.getNode(), root);

			formatElement(monitor);
			model.changedModel();
			model.save();
			getCommandStack().markSaveLocation();
			this.isCurrentlySaving = false;
		}
		catch (Exception e) {
		}
	}

	/**
	 * 
	 */
	public void doSaveAs() {
		performSaveAs();
	}

	/**
	 * @param monitor
	 */
	private void formatElement(IProgressMonitor monitor) {
		FormatProcessorXML formatProcessor = new FormatProcessorXML();
		formatProcessor.setProgressMonitor(monitor);
		formatProcessor.getFormatPreferences().setClearAllBlankLines(true);
		formatProcessor.formatModel(model);

		CleanupProcessorXML bla = new CleanupProcessorXML();
		bla.getCleanupPreferences().setCompressEmptyElementTags(true);
		bla.cleanupModel(model);
	}

	/**
	 * @param type
	 * @return
	 */
	public Object getAdapter(Class type) {
		if (type == IContentOutlinePage.class) {
			if (outlinePage == null) {
				outlinePage = new OutlinePage(new TreeViewer());
			}
			return outlinePage;
		}
		if (type == ZoomManager.class)
			return getGraphicalViewer().getProperty(
					ZoomManager.class.toString());
		if (type == IPropertySheetPage.class)
			return new TabbedPropertySheetPage(this);

		return super.getAdapter(type);
	}

	/**
	 * @return
	 */
	protected KeyHandler getCommonKeyHandler() {
		if (sharedKeyHandler == null) {
			sharedKeyHandler = new KeyHandler();
			sharedKeyHandler
					.put(KeyStroke.getPressed(SWT.DEL, 127, 0),
							getActionRegistry().getAction(
									ActionFactory.DELETE.getId()));
			sharedKeyHandler.put(KeyStroke.getPressed(SWT.F2, 0),
					getActionRegistry().getAction(
							GEFActionConstants.DIRECT_EDIT));
			// sharedKeyHandler.put(, 0), getActionRegistry()
			// .getAction(EditPropertiesAction.EDITPROPERTIES));
		}
		return sharedKeyHandler;
	}

	/**
	 * @return
	 */
	protected FigureCanvas getEditor() {
		return (FigureCanvas) getGraphicalViewer().getControl();
	}

	/**
	 * @return
	 */
	public GraphicalViewer getGraphViewer() {
		return getGraphicalViewer();
	}

	/**
	 * @return
	 * @see org.eclipse.gef.ui.parts.GraphicalEditorWithPalette#getPaletteRoot()
	 */
	protected PaletteRoot getPaletteRoot() {
		if (root == null)
			root = WebflowEditorPaletteFactory.createPalette();
		return root;
	}

	/**
	 * @param marker
	 */
	public void gotoMarker(IMarker marker) {
		System.out.println("");
	}

	/**
	 * @see org.eclipse.gef.ui.parts.GraphicalEditor#initializeGraphicalViewer()
	 */
	protected void initializeGraphicalViewer() {
		getGraphicalViewer().setContents(diagram);
	}

	/**
	 * @see org.eclipse.gef.ui.parts.GraphicalEditorWithPalette#initializePaletteViewer()
	 */
	/*
	 * protected void initializePaletteViewer() {
	 * super.initializePaletteViewer();
	 * getPaletteViewer().addDragSourceListener( new
	 * TemplateTransferDragSourceListener(getPaletteViewer())); }
	 */

	/**
	 * @return
	 * @see org.eclipse.ui.ISaveablePart#isDirty()
	 */
	public boolean isDirty() {
		return isSaveOnCloseNeeded();
	}

	/**
	 * @return
	 * @see org.eclipse.ui.ISaveablePart#isSaveAsAllowed()
	 */
	public boolean isSaveAsAllowed() {
		return true;
	}

	/**
	 * @return
	 * @see org.eclipse.ui.ISaveablePart#isSaveOnCloseNeeded()
	 */
	public boolean isSaveOnCloseNeeded() {
		return getCommandStack().isDirty();
	}

	/**
	 * @return
	 * @see org.eclipse.ui.ISaveablePart#doSaveAs()
	 */
	public boolean performSaveAs() {
		SaveAsDialog dialog = new SaveAsDialog(getSite().getWorkbenchWindow()
				.getShell());
		dialog.setOriginalFile(((WebflowEditorInput) getEditorInput())
				.getFile());
		dialog.open();
		IPath path = dialog.getResult();

		this.isCurrentlySaving = true;

		if (path == null)
			return false;

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final IFile file = workspace.getRoot().getFile(path);

		WorkspaceModifyOperation op = new WorkspaceModifyOperation() {

			@SuppressWarnings("restriction")
			public void execute(final IProgressMonitor monitor)
					throws CoreException {
				try {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					model.aboutToChangeModel();

					// reattach root node from document
					IDOMDocument document = ((DOMModelImpl) model)
							.getDocument();
					Node root = document.getDocumentElement();
					document.replaceChild(diagram.getNode(), root);

					formatElement(monitor);
					model.changedModel();
					model.save(out);
					file.create(new ByteArrayInputStream(out.toByteArray()),
							true, monitor);
					out.close();
					getCommandStack().markSaveLocation();
				}
				catch (Exception e) {
				}
			}
		};

		IWebflowConfig config = ((WebflowEditorInput) getEditorInput())
				.getConfig();
		IWebflowConfig newConfig = org.springframework.ide.eclipse.webflow.core.Activator
				.getModel().getProject(file.getProject()).getConfig(file);

		if (newConfig == null) {
			newConfig = new WebflowConfig(config.getProject());
			newConfig.setBeansConfigs(config.getBeansConfigs());
			newConfig.setResource(file);
			List<IWebflowConfig> configs = config.getProject().getConfigs();
			configs.add(newConfig);
			config.getProject().setConfigs(configs);
		}
		else {
			newConfig.setBeansConfigs(config.getBeansConfigs());
			List<IWebflowConfig> configs = config.getProject().getConfigs();
			config.getProject().setConfigs(configs);
		}

		try {
			new ProgressMonitorDialog(getSite().getWorkbenchWindow().getShell())
					.run(false, true, op);
			setInput(new WebflowEditorInput(newConfig));
			getCommandStack().markSaveLocation();
		}
		catch (Exception e) {
		}

		try {
			superSetInput(new WebflowEditorInput(newConfig));
			getCommandStack().markSaveLocation();
		}
		catch (Exception e) {
		}
		this.isCurrentlySaving = false;
		return true;
	}

	/**
	 * @return
	 */
	private boolean savePreviouslyNeeded() {
		return savePreviouslyNeeded;
	}

	/**
	 * @param input
	 * @see org.eclipse.ui.part.EditorPart#setInput(org.eclipse.ui.IEditorInput)
	 */
	protected void setInput(IEditorInput input) {
		superSetInput(input);
		this.file = ((WebflowEditorInput) input).getFile();
		setPartName(this.file.getName());

		try {
			model = null;
			model = StructuredModelManager.getModelManager()
					.getExistingModelForEdit(this.file);
			if (model == null) {
				model = StructuredModelManager.getModelManager()
						.getModelForEdit(this.file);

			}
			if (model != null) {
				IDOMDocument document = ((DOMModelImpl) model).getDocument();
				this.diagram = new WebflowState();
				this.diagram.init((IDOMNode) document.getDocumentElement()
						.cloneNode(true), null);
			}
		}
		catch (Exception e) {
		}
	}

	/**
	 * @param value
	 */
	private void setSavePreviouslyNeeded(boolean value) {
		savePreviouslyNeeded = value;
	}

	/**
	 * @param site
	 */
	protected void setSite(IWorkbenchPartSite site) {
		super.setSite(site);
		getSite().getWorkbenchWindow().getPartService().addPartListener(
				partListener);
	}

	/**
	 * @param input
	 */
	protected void superSetInput(IEditorInput input) {
		if (getEditorInput() != null) {
			IFile file = ((WebflowEditorInput) getEditorInput()).getFile();
			file.getWorkspace().removeResourceChangeListener(resourceListener);
		}

		super.setInput(input);

		if (getEditorInput() != null) {
			IFile file = ((WebflowEditorInput) getEditorInput()).getFile();
			file.getWorkspace().addResourceChangeListener(resourceListener);
			setPartName(file.getName());
		}
	}

	/**
	 * @return
	 */
	public String getContributorId() {
		return getSite().getId();
	}
}