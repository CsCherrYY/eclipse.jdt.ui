/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.search.SearchMatch;

import org.eclipse.jdt.internal.corext.refactoring.base.ReferencesInBinaryContext;
import org.eclipse.jdt.internal.corext.util.SearchUtils;

/**
 * Collects the results returned by a <code>SearchEngine</code>.
 * Only collects matches in CUs ands offers a scanner to trim match ranges.
 * If a {@link ReferencesInBinaryContext} is passed, matches that are
 * not inside a CU are added to the context.
 */
public abstract class CuCollectingSearchRequestor extends CollectingSearchRequestor {

	private final ReferencesInBinaryContext fBinaryRefs;
	
	private ICompilationUnit fCuCache;
	private IScanner fScannerCache;
	
	public CuCollectingSearchRequestor() {
		this(null);
	}

	public CuCollectingSearchRequestor(ReferencesInBinaryContext binaryRefs) {
		fBinaryRefs= binaryRefs;
	}

	protected IScanner getScanner(ICompilationUnit unit) {
		if (unit.equals(fCuCache))
			return fScannerCache;
		
		fCuCache= unit;
		IJavaProject project= unit.getJavaProject();
		String sourceLevel= project.getOption(JavaCore.COMPILER_SOURCE, true);
		String complianceLevel= project.getOption(JavaCore.COMPILER_COMPLIANCE, true);
		fScannerCache= ToolFactory.createScanner(false, false, false, sourceLevel, complianceLevel);
		return fScannerCache;
	}
	
	/**
	 * This is an internal method. Do not call from subclasses!
	 * Use {@link #collectMatch(SearchMatch)} instead.
	 * @param match 
	 * @throws CoreException 
	 * @deprecated
	 */
	public final void acceptSearchMatch(SearchMatch match) throws CoreException {
		if (filterMatch(match))
			return;
		
		ICompilationUnit unit= SearchUtils.getCompilationUnit(match);
		if (unit == null) {
			if (fBinaryRefs != null) {
				fBinaryRefs.add(match);
			}
			return;
		}
		acceptSearchMatch(unit, match);
	}
	
	public void collectMatch(SearchMatch match) throws CoreException {
		super.acceptSearchMatch(match);
	}
	
	/**
	 * @param match match
	 * @return <code>true</code> iff the given match should not be passed to {@link #acceptSearchMatch(ICompilationUnit, SearchMatch)}
	 */
	public boolean filterMatch(SearchMatch match) {
		return false;
	}
	
	protected abstract void acceptSearchMatch(ICompilationUnit unit, SearchMatch match) throws CoreException;

	public void endReporting() {
		fCuCache= null;
		fScannerCache= null;
	}
}


