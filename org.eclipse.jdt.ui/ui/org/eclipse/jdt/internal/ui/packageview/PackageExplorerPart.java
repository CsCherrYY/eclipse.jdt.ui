/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.packageview;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.OpenPerspectiveMenu;
import org.eclipse.ui.actions.OpenWithMenu;
import org.eclipse.ui.actions.RefreshAction;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.help.ViewContextComputer;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.part.ISetSelectionTarget;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.views.internal.framelist.BackAction;
import org.eclipse.ui.views.internal.framelist.ForwardAction;
import org.eclipse.ui.views.internal.framelist.FrameList;
import org.eclipse.ui.views.internal.framelist.GoIntoAction;
import org.eclipse.ui.views.internal.framelist.UpAction;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.IPackagesViewPart;
import org.eclipse.jdt.ui.JavaElementContentProvider;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ContextMenuGroup;
import org.eclipse.jdt.internal.ui.dnd.DelegatingDragAdapter;
import org.eclipse.jdt.internal.ui.dnd.DelegatingDropAdapter;
import org.eclipse.jdt.internal.ui.dnd.LocalSelectionTransfer;
import org.eclipse.jdt.internal.ui.dnd.TransferDragSourceListener;
import org.eclipse.jdt.internal.ui.dnd.TransferDropTargetListener;
import org.eclipse.jdt.internal.ui.javaeditor.ClassFileEditorInput;
import org.eclipse.jdt.internal.ui.javaeditor.JarEntryEditorInput;
import org.eclipse.jdt.internal.ui.preferences.JavaBasePreferencePage;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringGroup;
import org.eclipse.jdt.internal.ui.refactoring.actions.StructuredSelectionProvider;
import org.eclipse.jdt.internal.ui.reorg.DeleteAction;
import org.eclipse.jdt.internal.ui.reorg.ReorgGroup;
import org.eclipse.jdt.internal.ui.search.JavaSearchGroup;
import org.eclipse.jdt.internal.ui.util.OpenTypeHierarchyUtil;
import org.eclipse.jdt.internal.ui.viewsupport.MarkerErrorTickProvider;
import org.eclipse.jdt.internal.ui.viewsupport.ProblemTreeViewer;
import org.eclipse.jdt.internal.ui.viewsupport.StatusBarUpdater;
import org.eclipse.jdt.internal.ui.wizards.NewGroup;


/**
 * The ViewPart for the ProjectExplorer. It listens to part activation events.
 * When selection linking with the editor is enabled the view selection tracks
 * the active editor page. Similarly when a resource is selected in the packages
 * view the corresponding editor is activated. 
 */

public class PackageExplorerPart extends ViewPart implements ISetSelectionTarget, IMenuListener, IPackagesViewPart {
	
	public final static String VIEW_ID= JavaUI.ID_PACKAGES;
				
	// Persistance tags.
	static final String TAG_SELECTION= "selection"; //$NON-NLS-1$
	static final String TAG_EXPANDED= "expanded"; //$NON-NLS-1$
	static final String TAG_ELEMENT= "element"; //$NON-NLS-1$
	static final String TAG_PATH= "path"; //$NON-NLS-1$
	static final String TAG_VERTICAL_POSITION= "verticalPosition"; //$NON-NLS-1$
	static final String TAG_HORIZONTAL_POSITION= "horizontalPosition"; //$NON-NLS-1$
	static final String TAG_FILTERS = "filters"; //$NON-NLS-1$
	static final String TAG_FILTER = "filter"; //$NON-NLS-1$
	static final String TAG_SHOWLIBRARIES = "showLibraries"; //$NON-NLS-1$
	static final String TAG_SHOWBINARIES = "showBinaries"; //$NON-NLS-1$

	private JavaElementPatternFilter fPatternFilter= new JavaElementPatternFilter();
	private LibraryFilter fLibraryFilter= new LibraryFilter();
	private BinaryProjectFilter fBinaryFilter= new BinaryProjectFilter();

	private ProblemTreeViewer fViewer; 
	private PackagesFrameSource fFrameSource;
	private FrameList fFrameList;
	private ContextMenuGroup[] fStandardGroups;
	private Menu fContextMenu;		
	private OpenResourceAction fOpenCUAction;
	private Action fOpenToAction;
	private Action fShowTypeHierarchyAction;
	private Action fShowNavigatorAction;
	private Action fPropertyDialogAction;
 	private Action fDeleteAction;
 	private RefreshAction fRefreshAction;
 	private BackAction fBackAction;
	private ForwardAction fForwardAction;
	private GoIntoAction fZoomInAction;
	private UpAction fUpAction;
	private GotoTypeAction fGotoTypeAction;
	private GotoPackageAction fGotoPackageAction;
	private AddBookmarkAction fAddBookmarkAction;

 	private FilterSelectionAction fFilterAction;
 	private ShowLibrariesAction fShowLibrariesAction;
	private ShowBinariesAction fShowBinariesAction;
	private IMemento fMemento;
	
	private IPartListener fPartListener= new IPartListener() {
		public void partActivated(IWorkbenchPart part) {
			if (part instanceof IEditorPart)
				editorActivated((IEditorPart) part);
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


	public PackageExplorerPart() {
		super();		
	}

	/* (non-Javadoc)
	 * Method declared on IViewPart.
	 */
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		fMemento= memento;
	}
	
	/** 
	 * Initializes the default preferences
	 */
	public static void initDefaults(IPreferenceStore store) {
		store.setDefault(TAG_SHOWLIBRARIES, true);
		store.setDefault(TAG_SHOWBINARIES, true);
	}

	/**
	 * Returns the package explorer part of the active perspective. If 
	 * there isn't any package explorer part <code>null</code> is returned.
	 */
	public static PackageExplorerPart getFromActivePerspective() {
		IViewPart view= JavaPlugin.getActivePage().findView(VIEW_ID);
		if (view instanceof PackageExplorerPart)
			return (PackageExplorerPart)view;
		return null;	
	}
	
	/**
	 * Makes the package explorer part visible in the active perspective. If there
	 * isn't a package explorer part registered <code>null</code> is returned.
	 * Otherwise the opened view part is returned.
	 */
	public static PackageExplorerPart openInActivePerspective() {
		try {
			return (PackageExplorerPart)JavaPlugin.getActivePage().showView(VIEW_ID);
		} catch(PartInitException pe) {
			return null;
		}
	} 
		
	 public void dispose() {
	 	if (fViewer != null)
			JavaPlugin.getDefault().getProblemMarkerManager().removeListener(fViewer);
		if (fContextMenu != null && !fContextMenu.isDisposed())
			fContextMenu.dispose();
		getSite().getPage().removePartListener(fPartListener);
		super.dispose();	
	}
	/**
	 * Implementation of IWorkbenchPart.createPartControl(Composite)
	 */
	public void createPartControl(Composite parent) {
		fViewer= new ProblemTreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		
		fViewer.setContentProvider(new JavaElementContentProvider());
		JavaPlugin.getDefault().getProblemMarkerManager().addListener(fViewer);		

		int labelFlags= JavaElementLabelProvider.SHOW_BASICS | JavaElementLabelProvider.SHOW_OVERLAY_ICONS |
					JavaElementLabelProvider.SHOW_SMALL_ICONS | JavaElementLabelProvider.SHOW_VARIABLE;
		JavaElementLabelProvider labelProvider = new JavaElementLabelProvider(labelFlags);
		labelProvider.setErrorTickManager(new MarkerErrorTickProvider());
		fViewer.setLabelProvider(new DecoratingLabelProvider(labelProvider, null));
		fViewer.setSorter(new PackageViewerSorter());
		fViewer.addFilter(new EmptyInnerPackageFilter());
		fViewer.setUseHashlookup(true);
		fViewer.addFilter(fPatternFilter);
		fViewer.addFilter(fLibraryFilter);
		fViewer.addFilter(fBinaryFilter);
		if(fMemento != null) 
			restoreFilters();
		else
			initFilterFromPreferences();
			
		// Set input after filter and sorter has been set. This avoids resorting
		// and refiltering.
		fViewer.setInput(findInputElement());
		initDragAndDrop();
		initFrameList();
		initRefreshKey();
		updateTitle();
		
		MenuManager menuMgr= new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(this);
		fContextMenu= menuMgr.createContextMenu(fViewer.getTree());
		fViewer.getTree().setMenu(fContextMenu);
		getSite().registerContextMenu(menuMgr, fViewer);
		
		makeActions(); // call before registering for selection changes
			
		fViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				handleSelectionChanged(event);
			}
		});
		fViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				handleDoubleClick(event);
			}
		});
		fViewer.getControl().addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				handleKeyPressed(e);
			}
		});

		getSite().setSelectionProvider(fViewer);
		getSite().getPage().addPartListener(fPartListener);

		IStatusLineManager slManager= getViewSite().getActionBars().getStatusLineManager();
		fViewer.addSelectionChangedListener(new StatusBarUpdater(slManager));
		if (fMemento != null)
			restoreState(fMemento);
		fMemento= null;
		// Set help for the view 
		// fixed for 1GETAYN: ITPJUI:WIN - F1 help does nothing
		WorkbenchHelp.setHelp(fViewer.getControl(), new ViewContextComputer(this, IJavaHelpContextIds.PACKAGE_VIEW));
		
		fillActionBars();

	}

	private void fillActionBars() {
		IActionBars actionBars= getViewSite().getActionBars();
		IToolBarManager toolBar= actionBars.getToolBarManager();
		toolBar.add(fBackAction);
		toolBar.add(fForwardAction);
		toolBar.add(fUpAction);
		actionBars.updateActionBars();
	
		IMenuManager menu = actionBars.getMenuManager();
		menu.add(fFilterAction);
		menu.add(fShowLibrariesAction);  
		menu.add(fShowBinariesAction);  
	}
		
	private Object findInputElement() {
		Object input= getSite().getPage().getInput();
		if (input instanceof IWorkspace) { 
			return JavaCore.create(((IWorkspace)input).getRoot());
		} else if (input instanceof IContainer) {
			return JavaCore.create((IContainer)input);
		}
		//1GERPRT: ITPJUI:ALL - Packages View is empty when shown in Type Hierarchy Perspective
		// we can't handle the input
		// fall back to show the workspace
		return JavaCore.create(JavaPlugin.getWorkspace().getRoot());	
	}
	
	/**
	 * Answer the property defined by key.
	 */
	public Object getAdapter(Class key) {
		if (key.equals(ISelectionProvider.class))
			return fViewer;
		return super.getAdapter(key);
	}

	/**
	 * Returns the tool tip text for the given element.
	 */
	String getToolTipText(Object element) {
		if (element instanceof IResource) {
			IPath path= ((IResource) element).getFullPath();
			if (path.isRoot()) {
				return PackagesMessages.getString("PackageExplorer.title"); //$NON-NLS-1$
			}
			else {
				return path.makeRelative().toString();
			}
		}
		else {
			return ((ILabelProvider) getViewer().getLabelProvider()).getText(element);
		}
	}
	
	/**
	 * @see IWorkbenchPart#setFocus()
	 */
	public void setFocus() {
		fViewer.getTree().setFocus();
	}

	/**
	 * Returns the shell to use for opening dialogs.
	 * Used in this class, and in the actions.
	 */
	private Shell getShell() {
		return fViewer.getTree().getShell();
	}

	/**
	 * Returns the selection provider.
	 */
	private ISelectionProvider getSelectionProvider() {
		return fViewer;
	}
	
	/**
	 * Returns the current selection.
	 */
	private ISelection getSelection() {
		return fViewer.getSelection();
	}
	  
	//---- Action handling ----------------------------------------------------------
	
	/**
	 * Called when the context menu is about to open.
	 * Override to add your own context dependent menu contributions.
	 */
	public void menuAboutToShow(IMenuManager menu) {
		JavaPlugin.createStandardGroups(menu);
		IStructuredSelection selection= (IStructuredSelection) fViewer.getSelection();
		boolean selectionHasElements= !selection.isEmpty();
		Object element= selection.getFirstElement();
		// updateActions(selection);
		if (selection.size() == 1 && fViewer.isExpandable(element)) 
			menu.appendToGroup(IContextMenuConstants.GROUP_GOTO, fZoomInAction);
		addGotoMenu(menu);

		fOpenCUAction.update();
		if (fOpenCUAction.isEnabled())
			menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, fOpenCUAction);
			
		addOpenWithMenu(menu, selection);
		
		addOpenToMenu(menu, selection);
		addRefactoring(menu);
		
		// if (fShowTypeHierarchyAction.canActionBeAdded())
		//	menu.appendToGroup(IContextMenuConstants.GROUP_SHOW, fShowTypeHierarchyAction);
			
		// if (selectionHasElements)
		//	menu.appendToGroup(IContextMenuConstants.GROUP_SHOW, fShowNavigatorAction);
		
		ContextMenuGroup.add(menu, fStandardGroups, fViewer);
		
		if (fAddBookmarkAction.canOperateOnSelection())
			menu.appendToGroup(IContextMenuConstants.GROUP_REORGANIZE, fAddBookmarkAction);
					
		menu.appendToGroup(IContextMenuConstants.GROUP_BUILD, fRefreshAction);
		fRefreshAction.selectionChanged(selection);

		if (selectionHasElements) {
			// update the action to use the right selection since the refresh
			// action doesn't listen to selection changes.
			menu.appendToGroup(IContextMenuConstants.GROUP_PROPERTIES, fPropertyDialogAction);
		}	
	}

	 void addGotoMenu(IMenuManager menu) {
		MenuManager gotoMenu= new MenuManager(PackagesMessages.getString("PackageExplorer.gotoTitle")); //$NON-NLS-1$
		menu.appendToGroup(IContextMenuConstants.GROUP_GOTO, gotoMenu);
		gotoMenu.add(fBackAction);
		gotoMenu.add(fForwardAction);
		gotoMenu.add(fUpAction);
		gotoMenu.add(fGotoTypeAction);
		gotoMenu.add(fGotoPackageAction);
	}

	private void makeActions() {
		ISelectionProvider provider= getSelectionProvider();
		fOpenCUAction= new OpenResourceAction(provider);
		fPropertyDialogAction= new PropertyDialogAction(getShell(), provider);
		// fShowTypeHierarchyAction= new ShowTypeHierarchyAction(provider);
		fShowNavigatorAction= new ShowInNavigatorAction(provider);
		fAddBookmarkAction= new AddBookmarkAction(provider);
		
		fStandardGroups= new ContextMenuGroup[] {
			new NewGroup(),
			new BuildGroup(),
			new ReorgGroup(),
			new JavaSearchGroup()
		};
		
		fDeleteAction= new DeleteAction(StructuredSelectionProvider.createFrom(provider));
		fRefreshAction= new RefreshAction(getShell());
		fFilterAction = new FilterSelectionAction(getShell(), this, PackagesMessages.getString("PackageExplorer.filters")); //$NON-NLS-1$
		fShowLibrariesAction = new ShowLibrariesAction(getShell(), this, PackagesMessages.getString("PackageExplorer.referencedLibs")); //$NON-NLS-1$
		fShowBinariesAction = new ShowBinariesAction(getShell(), this, PackagesMessages.getString("PackageExplorer.binaryProjects")); //$NON-NLS-1$
		
		fBackAction= new BackAction(fFrameList);
		fForwardAction= new ForwardAction(fFrameList);
		fZoomInAction= new GoIntoAction(fFrameList);
		fUpAction= new UpAction(fFrameList);

		fGotoTypeAction= new GotoTypeAction(this);
		fGotoPackageAction= new GotoPackageAction(this);
		IActionBars actionService= getViewSite().getActionBars();
		actionService.setGlobalActionHandler(IWorkbenchActionConstants.DELETE, fDeleteAction);
	}
	

	private void addRefactoring(IMenuManager menu){
		MenuManager refactoring= new MenuManager(PackagesMessages.getString("PackageExplorer.refactoringTitle"));  //$NON-NLS-1$
		ContextMenuGroup.add(refactoring, new ContextMenuGroup[] { new RefactoringGroup() }, fViewer);
		if (!refactoring.isEmpty())
			menu.appendToGroup(IContextMenuConstants.GROUP_REORGANIZE, refactoring);
	}
	
	private void addOpenToMenu(IMenuManager menu, IStructuredSelection selection) {
		if (selection.size() != 1)
			return;
		IAdaptable element= (IAdaptable) selection.getFirstElement();
		IResource resource= (IResource)element.getAdapter(IResource.class);

		if ((resource instanceof IContainer)) {			
			// Create a menu flyout.
			MenuManager submenu = new MenuManager(PackagesMessages.getString("PackageExplorer.openPerspective")); //$NON-NLS-1$
			submenu.add(new OpenPerspectiveMenu(getSite().getWorkbenchWindow(), resource));
			menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, submenu);
		}
		OpenTypeHierarchyUtil.addToMenu(getSite().getWorkbenchWindow(), menu, element);
	}
	
	private void addOpenWithMenu(IMenuManager menu, IStructuredSelection selection) {
		// If one file is selected get it.
		// Otherwise, do not show the "open with" menu.
		if (selection.size() != 1)
			return;

		IAdaptable element= (IAdaptable)selection.getFirstElement();
		Object resource= element.getAdapter(IResource.class);
		if (!(resource instanceof IFile))
			return; 

		// Create a menu flyout.
		MenuManager submenu= new MenuManager(PackagesMessages.getString("PackageExplorer.openWith")); //$NON-NLS-1$
		submenu.add(new OpenWithMenu(getSite().getPage(), (IFile) resource));

		// Add the submenu.
		menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, submenu);

	}
	
	private boolean isSelectionOfType(ISelection s, Class clazz, boolean considerUnderlyingResource) {
		if (! (s instanceof IStructuredSelection) || s.isEmpty())
			return false;
		
		IStructuredSelection selection= (IStructuredSelection)s;
		Iterator iter= selection.iterator();
		while (iter.hasNext()) {
			Object o= iter.next();
			if (clazz.isInstance(o))
				return true;
			if (considerUnderlyingResource) {
				if (! (o instanceof IJavaElement))
					return false;
				IJavaElement element= (IJavaElement)o;
				Object resource= element.getAdapter(IResource.class);
				if (! clazz.isInstance(resource))
					return false;
			}
		}
		return true;	
	}
	
	//---- Event handling ----------------------------------------------------------
	
	private void initDragAndDrop() {
		int ops= DND.DROP_COPY | DND.DROP_MOVE;
		final LocalSelectionTransfer lt= LocalSelectionTransfer.getInstance();
		Transfer[] transfers= new Transfer[] {lt, FileTransfer.getInstance()};
		
		// Drop Adapter
		TransferDropTargetListener[] dropListeners= new TransferDropTargetListener[] {
			new SelectionTransferDropAdapter(fViewer),
			new FileTransferDropAdapter(fViewer)
		};
		fViewer.addDropSupport(ops, transfers, new DelegatingDropAdapter(dropListeners));
		
		// Drag Adapter
		Control control= fViewer.getControl();
		TransferDragSourceListener[] dragListeners= new TransferDragSourceListener[] {
			new SelectionTransferDragAdapter(fViewer),
			new FileTransferDragAdapter(fViewer)
		};
		DragSource source= new DragSource(control, ops);
		// Note, that the transfer agents are set by the delegating drag adapter itself.
		source.addDragListener(new DelegatingDragAdapter(dragListeners));
	}

	/**
 	 * Handles key events in viewer.
 	 */
	void handleKeyPressed(KeyEvent event) {
		if (event.character == SWT.DEL && event.stateMask == 0 && fDeleteAction.isEnabled())
			fDeleteAction.run();
	}
	
	/**
	 * Handles double clicks in viewer.
	 * Opens editor if file double-clicked.
	 */
	private void handleDoubleClick(DoubleClickEvent event) {
		IStructuredSelection s= (IStructuredSelection) event.getSelection();
		Object element= s.getFirstElement();
		if (fViewer.isExpandable(element)) {
			if (JavaBasePreferencePage.doubleClockGoesInto())
				fZoomInAction.run();
			else
				fViewer.setExpandedState(element, !fViewer.getExpandedState(element));
			return;
		}
		if (fOpenCUAction.isEnabled()) {
			fOpenCUAction.run();
			return;
		}
	}

	/**
	 * Handles selection changed in viewer.
	 * Updates global actions.
	 * Links to editor (if option enabled)
	 */
	private void handleSelectionChanged(SelectionChangedEvent event) {
		IStructuredSelection sel= (IStructuredSelection) event.getSelection();
		//updateGlobalActions(sel);
		fZoomInAction.update();
		linkToEditor(sel);
	}

	public void selectReveal(ISelection selection) {
		ISelection javaSelection= convertSelection(selection);
	 	fViewer.setSelection(javaSelection, true);
	}

	private ISelection convertSelection(ISelection s) {
		List converted= new ArrayList();
		if (s instanceof StructuredSelection) {
			Object[] elements= ((StructuredSelection)s).toArray();
			for (int i= 0; i < elements.length; i++) {
				Object e= elements[i];
				if (e instanceof IJavaElement)	
					converted.add(e);
				else if (e instanceof IResource) {
					IJavaElement element= JavaCore.create((IResource)e);
					if (element != null) 
						converted.add(element);
					else 
						converted.add(e);
				}
			}
		}
		return new StructuredSelection(converted.toArray());
	}
	
	public void selectAndReveal(Object element) {
		selectReveal(new StructuredSelection(element));
	}
	
	/**
	 * Returns whether the preference to link selection to active editor is enabled.
	 */
	boolean isLinkingEnabled() {
		return JavaBasePreferencePage.linkPackageSelectionToEditor();
	}

	/**
	 * Links to editor (if option enabled)
	 */
	private void linkToEditor(IStructuredSelection selection) {	
		if (!isLinkingEnabled())
			return; 

		Object obj= selection.getFirstElement();
		Object element= null;

		if (selection.size() == 1) {
			if (obj instanceof ICompilationUnit) 
				element= getResourceFor(obj);
			else if (obj instanceof IClassFile) 
				element= obj;
			else if (obj instanceof IFile)
				element= obj;
				
			if (element == null)
				return;

			IWorkbenchPage page= getSite().getPage();
			IEditorPart editorArray[]= page.getEditors();
			for (int i= 0; i < editorArray.length; ++i) {
				IEditorPart editor= editorArray[i];
				Object input= getElementOfInput(editor.getEditorInput());					
				if (input != null && input.equals(element)) {
					page.bringToTop(editor);
					return;
				}
			}
		}
	}

	private IResource getResourceFor(Object element) {
		if (element instanceof IJavaElement) {
			try {
				element= ((IJavaElement)element).getCorrespondingResource();
			} catch (JavaModelException e) {
				return null;
			}
		}
		if (!(element instanceof IResource) || ((IResource)element).isPhantom()) {
			return null;
		}
		return (IResource)element;
	}
	
	public void saveState(IMemento memento) {
		if (fViewer == null) {
			// part has not been created
			if (fMemento != null) //Keep the old state;
				memento.putMemento(fMemento);
			return;
		}
		Tree tree= fViewer.getTree();
		Object expandedElements[]= fViewer.getExpandedElements();
		if (expandedElements.length > 0) {
			IMemento expandedMem= memento.createChild(TAG_EXPANDED);
			for (int i= 0; i < expandedElements.length; i++) {
				IMemento elementMem= expandedMem.createChild(TAG_ELEMENT);
				// we can only persist JavaElements for now
				Object o= expandedElements[i];
				if (o instanceof IJavaElement)
					elementMem.putString(TAG_PATH, ((IJavaElement) expandedElements[i]).getHandleIdentifier());
			}
		}
		Object elements[]= ((IStructuredSelection) fViewer.getSelection()).toArray();
		if (elements.length > 0) {
			IMemento selectionMem= memento.createChild(TAG_SELECTION);
			for (int i= 0; i < elements.length; i++) {
				IMemento elementMem= selectionMem.createChild(TAG_ELEMENT);
				// we can only persist JavaElements for now
				Object o= elements[i];
				if (o instanceof IJavaElement)
					elementMem.putString(TAG_PATH, ((IJavaElement) elements[i]).getHandleIdentifier());
			}
		}

		//save vertical position
		ScrollBar bar= tree.getVerticalBar();
		int position= bar != null ? bar.getSelection() : 0;
		memento.putString(TAG_VERTICAL_POSITION, String.valueOf(position));
		//save horizontal position
		bar= tree.getHorizontalBar();
		position= bar != null ? bar.getSelection() : 0;
		memento.putString(TAG_HORIZONTAL_POSITION, String.valueOf(position));

		//save filters
		String filters[] = getPatternFilter().getPatterns();
		if(filters.length > 0) {
			IMemento filtersMem = memento.createChild(TAG_FILTERS);
			for (int i = 0; i < filters.length; i++){
				IMemento child = filtersMem.createChild(TAG_FILTER);
				child.putString(TAG_ELEMENT,filters[i]);
			}
		}
		//save library filter
		boolean showLibraries= getLibraryFilter().getShowLibraries();
		String show= "true"; //$NON-NLS-1$
		if (!showLibraries)
			show= "false"; //$NON-NLS-1$
		memento.putString(TAG_SHOWLIBRARIES, show);
		
		//save binary filter
		boolean showBinaries= getBinaryFilter().getShowBinaries();
		String showBinString= "true"; //$NON-NLS-1$
		if (!showBinaries)
			showBinString= "false"; //$NON-NLS-1$
		memento.putString(TAG_SHOWBINARIES, showBinString);
	
	}

	void restoreState(IMemento memento) {
		// restore expansion state
		IMemento childMem= memento.getChild(TAG_EXPANDED);
		if (childMem != null) {
			ArrayList elements= new ArrayList();
			IMemento[] elementMem= childMem.getChildren(TAG_ELEMENT);
			for (int i= 0; i < elementMem.length; i++) {
				Object element= JavaCore.create(elementMem[i].getString(TAG_PATH));
				elements.add(element);
			}
			fViewer.setExpandedElements(elements.toArray());
		}
		// restoreSelection
		childMem= memento.getChild(TAG_SELECTION);
		if (childMem != null) {
			ArrayList list= new ArrayList();
			IMemento[] elementMem= childMem.getChildren(TAG_ELEMENT);
			for (int i= 0; i < elementMem.length; i++) {
				Object element= JavaCore.create(elementMem[i].getString(TAG_PATH));
				list.add(element);
			}
			fViewer.setSelection(new StructuredSelection(list));
		}

		Tree tree= fViewer.getTree();
		//save vertical position
		ScrollBar bar= tree.getVerticalBar();
		if (bar != null) {
			try {
				String posStr= memento.getString(TAG_VERTICAL_POSITION);
				int position;
				position= new Integer(posStr).intValue();
				bar.setSelection(position);
			} catch (NumberFormatException e) {
			}
		}
		bar= tree.getHorizontalBar();
		if (bar != null) {
			try {
				String posStr= memento.getString(TAG_HORIZONTAL_POSITION);
				int position;
				position= new Integer(posStr).intValue();
				bar.setSelection(position);
			} catch (NumberFormatException e) {
			}
		}
	}
	
	/**
	 * Create the KeyListener for doing the refresh on the viewer.
	 */
	private void initRefreshKey() {
		fViewer.getControl().addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent event) {
				if (event.keyCode == SWT.F5) {
					fRefreshAction.selectionChanged(
						(IStructuredSelection) fViewer.getSelection());
					if (fRefreshAction.isEnabled())
						fRefreshAction.run();
				}
			}
		});
	}
	
	void initFrameList() {
		fFrameSource= new PackagesFrameSource(this);
		fFrameList= new FrameList(fFrameSource);
		fFrameSource.connectTo(fFrameList);
	}


	/**
	 * An editor has been activated.  Set the selection in this Packages Viewer
	 * to be the editor's input, if linking is enabled.
	 */
	void editorActivated(IEditorPart editor) {
		if (!isLinkingEnabled())  
			return;
		Object input= getElementOfInput(editor.getEditorInput());
		Object element= null;
		
		if (input instanceof IFile) 
			element= JavaCore.create((IFile)input);
			
		if (element == null) // try a non Java resource
			element= input;
			
		if (element != null) {
			ISelection newSelection= new StructuredSelection(element);
			if (!fViewer.getSelection().equals(newSelection)) {
				 fViewer.setSelection(newSelection);
			}
		}
	}
	
	/**
	 * Returns the element contained in the EditorInput
	 */
	Object getElementOfInput(IEditorInput input) {
		if (input instanceof IFileEditorInput)
			return ((IFileEditorInput)input).getFile();
		else if (input instanceof ClassFileEditorInput)
			return ((ClassFileEditorInput)input).getClassFile();
		else if (input instanceof JarEntryEditorInput)
			return ((JarEntryEditorInput)input).getStorage();
		return null;
	}
	
	/**
 	 * Returns the Viewer.
 	 */
	TreeViewer getViewer() {
		return fViewer;
	}
	
	/**
 	 * Returns the pattern filter for this view.
 	 * @return the pattern filter
 	 */
	JavaElementPatternFilter getPatternFilter() {
		return fPatternFilter;
	}
	
	/**
 	 * Returns the library filter for this view.
 	 * @return the library filter
 	 */
	LibraryFilter getLibraryFilter() {
		return fLibraryFilter;
	}

	/**
 	 * Returns the Binary filter for this view.
 	 * @return the binary filter
 	 */
	BinaryProjectFilter getBinaryFilter() {
		return fBinaryFilter;
	}

	void restoreFilters() {
		IMemento filtersMem= fMemento.getChild(TAG_FILTERS);
		if(filtersMem != null) {	
			IMemento children[]= filtersMem.getChildren(TAG_FILTER);
			String filters[]= new String[children.length];
			for (int i = 0; i < children.length; i++) {
				filters[i]= children[i].getString(TAG_ELEMENT);
			}
			getPatternFilter().setPatterns(filters);
		} else {
			getPatternFilter().setPatterns(new String[0]);
		}
		//restore library
		String show= fMemento.getString(TAG_SHOWLIBRARIES);
		if (show != null)
			getLibraryFilter().setShowLibraries(show.equals("true")); //$NON-NLS-1$
		else 
			initLibraryFilterFromPreferences();		
		
		//restore binary fileter
		String showbin= fMemento.getString(TAG_SHOWBINARIES);
		if (showbin != null)
			getBinaryFilter().setShowBinaries(show.equals("true")); //$NON-NLS-1$
		else 
			initBinaryFilterFromPreferences();		
	}
	
	void initFilterFromPreferences() {
		initBinaryFilterFromPreferences();
		initLibraryFilterFromPreferences();
	}

	void initLibraryFilterFromPreferences() {
		JavaPlugin plugin= JavaPlugin.getDefault();
		boolean show= plugin.getPreferenceStore().getBoolean(TAG_SHOWLIBRARIES);
		getLibraryFilter().setShowLibraries(show);
	}

	void initBinaryFilterFromPreferences() {
		JavaPlugin plugin= JavaPlugin.getDefault();
		boolean showbin= plugin.getPreferenceStore().getBoolean(TAG_SHOWBINARIES);
		getBinaryFilter().setShowBinaries(showbin);
	}
	/**
	 * Updates the title text and title tool tip.
	 * Called whenever the input of the viewer changes.
	 */ 
	void updateTitle() {		
		Object input= getViewer().getInput();
		String viewName= getConfigurationElement().getAttribute("name"); //$NON-NLS-1$
		if (input == null
			|| (input instanceof IJavaModel)) {
			setTitle(viewName);
			setTitleToolTip(""); //$NON-NLS-1$
		} else {
			ILabelProvider labelProvider = (ILabelProvider) getViewer().getLabelProvider();
			String inputText= labelProvider.getText(input);
			String title= PackagesMessages.getFormattedString("PackageExplorer.argTitle", new String[] { viewName, inputText }); //$NON-NLS-1$
			setTitle(title);
			setTitleToolTip(getToolTipText(input));
		} 
	}
	
	/**
	 * Sets the decorator for the package explorer.
	 *
	 * @param decorator a label decorator or <code>null</code> for no decorations.
	 */
	public void setLabelDecorator(ILabelDecorator decorator) {
		int labelFlags= JavaElementLabelProvider.SHOW_BASICS | JavaElementLabelProvider.SHOW_OVERLAY_ICONS |
					JavaElementLabelProvider.SHOW_SMALL_ICONS | JavaElementLabelProvider.SHOW_VARIABLE;
		JavaElementLabelProvider javaProvider= new JavaElementLabelProvider(labelFlags);
		javaProvider.setErrorTickManager(new MarkerErrorTickProvider());
		if (decorator == null) {
			fViewer.setLabelProvider(javaProvider);
		} else {
			fViewer.setLabelProvider(new DecoratingLabelProvider(javaProvider, decorator));
		}
	}
}
