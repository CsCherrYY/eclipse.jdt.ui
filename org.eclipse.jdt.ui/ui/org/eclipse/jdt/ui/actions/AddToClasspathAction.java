/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Action to add a JAR to the classpath of its parent project.
 * Action is applicable to selections containing archives (JAR or zip) 
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.1
 */
public class AddToClasspathAction extends SelectionDispatchAction {

	/**
	 * Creates a new <code>AddToClasspathAction</code>. The action requires that
	 * the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public AddToClasspathAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.getString("AddToClasspathAction.label")); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("AddToClasspathAction.toolTip")); //$NON-NLS-1$
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.ADD_TO_CLASSPATH_ACTION);
	}
	
	/* (non-Javadoc)
	 * Method declared in SelectionDispatchAction
	 */
	public void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(checkEnabled(selection));
		} catch (JavaModelException e) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.filterNotPresentException(e))
				JavaPlugin.log(e);
			setEnabled(false);
		}
	}
	
	private static boolean checkEnabled(IStructuredSelection selection) throws JavaModelException {
		if (selection.isEmpty())
			return false;
		for (Iterator iter= selection.iterator(); iter.hasNext();) {
			if (! canBeAddedToBuildPath(iter.next()))
				return false;
		}
		return true;
	}

	private static boolean canBeAddedToBuildPath(Object element) throws JavaModelException{
		return (element instanceof IAdaptable) && getCandidate((IAdaptable) element) != null;
	}

	private static IFile getCandidate(IAdaptable element) throws JavaModelException {
		IResource resource= (IResource)element.getAdapter(IResource.class);
		if (! (resource instanceof IFile))
			return null;
		
		IJavaProject project= JavaCore.create(resource.getProject());
		if (project != null && project.exists() && (project.findPackageFragmentRoot(resource.getFullPath()) == null))
			return (IFile) resource;
		return null;
	}
			
	/* (non-Javadoc)
	 * Method declared in SelectionDispatchAction
	 */
	public void run(IStructuredSelection selection) {
		try {
			final IFile[] files= getJARFiles(selection);
			IResource rule= null;
			for (int i= 0; i < files.length; i++) {
				IProject curr= files[i].getProject();
				if (rule == null) {
					rule= curr;
				} else if (!rule.equals(curr)) {
					rule= curr.getParent();
				}
			}
			
			
			IWorkspaceRunnable operation= new IWorkspaceRunnable() {
				public void run(IProgressMonitor monitor) throws CoreException {
					monitor.beginTask(ActionMessages.getString("AddToClasspathAction.progressMessage"), files.length); //$NON-NLS-1$
					for (int i= 0; i < files.length; i++) {
						monitor.subTask(files[i].getFullPath().toString());
						IJavaProject project= JavaCore.create(files[i].getProject());
						addToClassPath(project, files[i].getFullPath(), new SubProgressMonitor(monitor, 1));
					}
				}
				
				private void addToClassPath(IJavaProject project, IPath jarPath, IProgressMonitor monitor) throws JavaModelException {
					if (monitor.isCanceled())
						throw new OperationCanceledException();
					IClasspathEntry[] entries= project.getRawClasspath();
					IClasspathEntry[] newEntries= new IClasspathEntry[entries.length + 1];
					System.arraycopy(entries, 0, newEntries, 0, entries.length);
					newEntries[entries.length]= JavaCore.newLibraryEntry(jarPath, null, null, false);
					project.setRawClasspath(newEntries, monitor);
				}
			};	
			
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().run(true, true, new WorkbenchRunnableAdapter(operation, rule));
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, getShell(), 
				ActionMessages.getString("AddToClasspathAction.error.title"),  //$NON-NLS-1$
				ActionMessages.getString("AddToClasspathAction.error.message")); //$NON-NLS-1$
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, getShell(), 
					ActionMessages.getString("AddToClasspathAction.error.title"),  //$NON-NLS-1$
					ActionMessages.getString("AddToClasspathAction.error.message")); //$NON-NLS-1$
		} catch (InterruptedException e) {
			// canceled
		}

	}
	
	private static IFile[] getJARFiles(IStructuredSelection selection) throws JavaModelException {
		ArrayList list= new ArrayList();
		for (Iterator iter= selection.iterator(); iter.hasNext();) {
			Object element= iter.next();
			if (element instanceof IAdaptable) {
				IFile file= getCandidate((IAdaptable) element);
				if (file != null) {
					list.add(file);
				}
			}
		}
		return (IFile[]) list.toArray(new IFile[list.size()]);
	}
}

