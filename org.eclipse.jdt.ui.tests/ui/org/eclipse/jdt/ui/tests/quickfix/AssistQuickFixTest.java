/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sebastian Davids <sdavids@gmx.de> - testInvertEquals1-23
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.Hashtable;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.Preferences;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.AssignToVariableAssistProposal;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.LinkedNamesAssistProposal;

public class AssistQuickFixTest extends QuickFixTest {
	
	private static final Class THIS= AssistQuickFixTest.class;
	
	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	public AssistQuickFixTest(String name) {
		super(name);
	}
	
	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}
	
	public static Test suite() {
		if (true) {
			return allTests();
		} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new AssistQuickFixTest("testAssignToLocal7"));
			return new ProjectTestSetup(suite);
		}
	}

	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}

	protected void setUp() throws Exception {
		Hashtable options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		
		JavaCore.setOptions(options);			

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);
		store.setValue(PreferenceConstants.CODEGEN_KEYWORD_THIS, false);
		
		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "//TODO\n${body_statement}", null);
		
		Preferences corePrefs= JavaCore.getPlugin().getPluginPreferences();
		corePrefs.setValue(JavaCore.CODEASSIST_FIELD_PREFIXES, "");
		corePrefs.setValue(JavaCore.CODEASSIST_STATIC_FIELD_PREFIXES, "");
		corePrefs.setValue(JavaCore.CODEASSIST_FIELD_SUFFIXES, "");
		corePrefs.setValue(JavaCore.CODEASSIST_STATIC_FIELD_SUFFIXES, "");		
		
		fJProject1= ProjectTestSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}
	
	public void testAssignToLocal() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        getClass();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
				
		int offset= buf.toString().indexOf("getClass()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private Class<? extends E> class1;\n");
		buf.append("\n");
		buf.append("    public void foo() {\n");
		buf.append("        class1 = getClass();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        Class<? extends E> class1 = getClass();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });	
		
	}

	public void testAssignToLocal2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");		
		buf.append("public class E {\n");
		buf.append("    public Vector goo() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");		
		buf.append("    public void foo() {\n");
		buf.append("        goo().iterator();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
				
		int offset= buf.toString().indexOf("goo().iterator()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.Vector;\n");				
		buf.append("public class E {\n");
		buf.append("    private Iterator iterator;\n");
		buf.append("    public Vector goo() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");		
		buf.append("    public void foo() {\n");
		buf.append("        iterator = goo().iterator();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.Vector;\n");				
		buf.append("public class E {\n");
		buf.append("    public Vector goo() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");		
		buf.append("    public void foo() {\n");
		buf.append("        Iterator iterator = goo().iterator();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });	
	}
	
	public void testAssignToLocal3() throws Exception {
		// test prefixes and this qualification
		
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_KEYWORD_THIS, true);
		Preferences corePrefs= JavaCore.getPlugin().getPluginPreferences();
		corePrefs.setValue(JavaCore.CODEASSIST_FIELD_PREFIXES, "f");
		corePrefs.setValue(JavaCore.CODEASSIST_LOCAL_PREFIXES, "_");
			
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    private int fCount;\n");
		buf.append("\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.getSecurityManager();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
				
		int offset= buf.toString().indexOf("System");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    private int fCount;\n");
		buf.append("    private SecurityManager fSecurityManager;\n");
		buf.append("\n");				
		buf.append("    public void foo() {\n");
		buf.append("        this.fSecurityManager = System.getSecurityManager();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    private int fCount;\n");
		buf.append("\n");
		buf.append("    public void foo() {\n");
		buf.append("        SecurityManager _securityManager = System.getSecurityManager();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });	
	}
	
	public void testAssignToLocal4() throws Exception {
		// test name conflict
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    private int f;\n");
		buf.append("\n");
		buf.append("    public void foo() {\n");
		buf.append("        Math.min(1.0f, 2.0f);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
				
		int offset= buf.toString().indexOf("Math");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    private int f;\n");
		buf.append("    private float g;\n");
		buf.append("\n");				
		buf.append("    public void foo() {\n");
		buf.append("        g = Math.min(1.0f, 2.0f);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    private int f;\n");
		buf.append("\n");
		buf.append("    public void foo() {\n");
		buf.append("        float g = Math.min(1.0f, 2.0f);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });	
	}

	public void testAssignToLocal5() throws Exception {
		// test prefixes and this qualification on static method
		
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_KEYWORD_THIS, true);
		Preferences corePrefs= JavaCore.getPlugin().getPluginPreferences();
		corePrefs.setValue(JavaCore.CODEASSIST_FIELD_PREFIXES, "f");
		corePrefs.setValue(JavaCore.CODEASSIST_STATIC_FIELD_PREFIXES, "fg");
		corePrefs.setValue(JavaCore.CODEASSIST_LOCAL_PREFIXES, "_");
			
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    private int fCount;\n");
		buf.append("\n");
		buf.append("    public static void foo() {\n");
		buf.append("        System.getSecurityManager();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
				
		int offset= buf.toString().indexOf("System");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    private int fCount;\n");
		buf.append("    private static SecurityManager fgSecurityManager;\n");
		buf.append("\n");				
		buf.append("    public static void foo() {\n");
		buf.append("        E.fgSecurityManager = System.getSecurityManager();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    private int fCount;\n");
		buf.append("\n");
		buf.append("    public static void foo() {\n");
		buf.append("        SecurityManager _securityManager = System.getSecurityManager();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });	
	}

	public void testAssignToLocal6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    static {\n");
		buf.append("        getClass(); // comment\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		int offset= buf.toString().indexOf("getClass()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private static Class<? extends E> class1;\n");
		buf.append("\n");
		buf.append("    static {\n");
		buf.append("        class1 = getClass(); // comment\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    static {\n");
		buf.append("        Class<? extends E> class1 = getClass(); // comment\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });	
	}
	
	public void testAssignToLocal7() throws Exception {
		// test name conflict: name used later
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");		
		buf.append("public class E {\n");
		buf.append("    public Vector goo() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");		
		buf.append("    public void foo() {\n");
		buf.append("        goo().iterator();\n");
		buf.append("        Object iterator= null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
				
		int offset= buf.toString().indexOf("goo().iterator()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.Vector;\n");				
		buf.append("public class E {\n");
		buf.append("    private Iterator iterator2;\n");
		buf.append("    public Vector goo() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");		
		buf.append("    public void foo() {\n");
		buf.append("        iterator2 = goo().iterator();\n");
		buf.append("        Object iterator= null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.Vector;\n");				
		buf.append("public class E {\n");
		buf.append("    public Vector goo() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");		
		buf.append("    public void foo() {\n");
		buf.append("        Iterator iterator2 = goo().iterator();\n");
		buf.append("        Object iterator= null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });	
	}


	
	public void testAssignParamToField() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public  E(int count) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
				
		String selection= "count";
		int offset= buf.toString().indexOf(selection);
		AssistContext context= getCorrectionContext(cu, offset, selection.length());
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private final int count;\n");
		buf.append("\n");
		buf.append("    public  E(int count) {\n");
		buf.append("        this.count = count;\n");		
		buf.append("    }\n");
		buf.append("}\n");
		String ex1= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public  E(final int count) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String ex2= buf.toString();
		
		assertExpectedExistInProposals(proposals, new String[] {ex1, ex2});
	}
	
	public void testAssignParamToField2() throws Exception {
		Preferences corePrefs= JavaCore.getPlugin().getPluginPreferences();
		corePrefs.setValue(JavaCore.CODEASSIST_FIELD_PREFIXES, "f");

		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    public  E(int count, Vector vec[]) {\n");
		buf.append("        super();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
				
		String selection= "vec";
		int offset= buf.toString().indexOf(selection);
		AssistContext context= getCorrectionContext(cu, offset, selection.length());
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    private final Vector[] fVec;\n");
		buf.append("\n");
		buf.append("    public  E(int count, Vector vec[]) {\n");
		buf.append("        super();\n");
		buf.append("        fVec = vec;\n");		
		buf.append("    }\n");
		buf.append("}\n");
		String ex1= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    public  E(int count, final Vector vec[]) {\n");
		buf.append("        super();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String ex2= buf.toString();
		
		assertExpectedExistInProposals(proposals, new String[] {ex1, ex2});
	}

	public void testAssignParamToField3() throws Exception {
		Preferences corePrefs= JavaCore.getPlugin().getPluginPreferences();
		corePrefs.setValue(JavaCore.CODEASSIST_STATIC_FIELD_PREFIXES, "fg");

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_KEYWORD_THIS, true);
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    private int fgVec;\n");
		buf.append("\n");
		buf.append("    public static void foo(int count, Vector vec[]) {\n");
		buf.append("        count++;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
				
		String selection= "vec";
		int offset= buf.toString().indexOf(selection);
		AssistContext context= getCorrectionContext(cu, offset, selection.length());
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    private int fgVec;\n");
		buf.append("    private static Vector[] fgVec2;\n");
		buf.append("\n");
		buf.append("    public static void foo(int count, Vector vec[]) {\n");
		buf.append("        E.fgVec2 = vec;\n");
		buf.append("        count++;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String ex1= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    private int fgVec;\n");
		buf.append("\n");
		buf.append("    public static void foo(int count, final Vector vec[]) {\n");
		buf.append("        count++;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String ex2= buf.toString();
		
		assertExpectedExistInProposals(proposals, new String[] {ex1, ex2});
	}
	
	public void testAssignParamToField4() throws Exception {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_KEYWORD_THIS, true);
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private long count;\n");
		buf.append("\n");
		buf.append("    public void foo(int count) {\n");
		buf.append("        count++;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
				
		int offset= buf.toString().indexOf("int count");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private long count;\n");
		buf.append("    private int count2;\n");
		buf.append("\n");
		buf.append("    public void foo(int count) {\n");
		buf.append("        this.count2 = count;\n");
		buf.append("        count++;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private long count;\n");
		buf.append("\n");
		buf.append("    public void foo(int count) {\n");
		buf.append("        this.count = count;\n");
		buf.append("        count++;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });	
	}
	
	public void testAssignParamToField5() throws Exception {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_KEYWORD_THIS, true);
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private int p1;\n");
		buf.append("\n");
		buf.append("    public void foo(int p1, int p2) {\n");
		buf.append("        this.p1 = p1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		int offset= buf.toString().indexOf("int p2");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private int p1;\n");
		buf.append("    private int p2;\n");
		buf.append("\n");
		buf.append("    public void foo(int p1, int p2) {\n");
		buf.append("        this.p1 = p1;\n");
		buf.append("        this.p2 = p2;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private int p1;\n");
		buf.append("\n");
		buf.append("    public void foo(int p1, int p2) {\n");
		buf.append("        this.p1 = p1;\n");
		buf.append("        this.p1 = p2;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });	
	}
	
	public void testAssignParamToField6() throws Exception {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_KEYWORD_THIS, true);
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private Float p1;\n");
		buf.append("    private Number p2;\n");
		buf.append("\n");
		buf.append("    public void foo(Float p1, Integer p2) {\n");
		buf.append("        this.p1 = p1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		int offset= buf.toString().indexOf("Integer p2");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private Float p1;\n");
		buf.append("    private Number p2;\n");
		buf.append("\n");
		buf.append("    public void foo(Float p1, Integer p2) {\n");
		buf.append("        this.p1 = p1;\n");
		buf.append("        this.p2 = p2;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private Float p1;\n");
		buf.append("    private Number p2;\n");
		buf.append("    private Integer p22;\n");
		buf.append("\n");
		buf.append("    public void foo(Float p1, Integer p2) {\n");
		buf.append("        this.p1 = p1;\n");
		buf.append("        this.p22 = p2;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });	
	}	
	
	public void testAssignParamToFieldInGeneric() throws Exception {
		Preferences corePrefs= JavaCore.getPlugin().getPluginPreferences();
		corePrefs.setValue(JavaCore.CODEASSIST_FIELD_PREFIXES, "f");

		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E<T> {\n");
		buf.append("    public  E(int count, Vector<String>[] vec) {\n");
		buf.append("        super();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
				
		String selection= "vec";
		int offset= buf.toString().indexOf(selection);
		AssistContext context= getCorrectionContext(cu, offset, selection.length());
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E<T> {\n");
		buf.append("    private final Vector<String>[] fVec;\n");
		buf.append("\n");
		buf.append("    public  E(int count, Vector<String>[] vec) {\n");
		buf.append("        super();\n");
		buf.append("        fVec = vec;\n");		
		buf.append("    }\n");
		buf.append("}\n");
		String ex1= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E<T> {\n");
		buf.append("    public  E(int count, final Vector<String>[] vec) {\n");
		buf.append("        super();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String ex2= buf.toString();
		
		assertExpectedExistInProposals(proposals, new String[] {ex1, ex2});
	}
	
	public void testAssignToLocal2CursorAtEnd() throws Exception {	
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");		
		buf.append("public class E {\n");
		buf.append("    public Vector goo() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");		
		buf.append("    public void foo() {\n");
		buf.append("        goo().toArray();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "goo().toArray();";
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str) + str.length(), 0);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");				
		buf.append("public class E {\n");
		buf.append("    private Object[] objects;\n");
		buf.append("    public Vector goo() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");		
		buf.append("    public void foo() {\n");
		buf.append("        objects = goo().toArray();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");				
		buf.append("public class E {\n");
		buf.append("    public Vector goo() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");		
		buf.append("    public void foo() {\n");
		buf.append("        Object[] objects = goo().toArray();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });	
	}
	
	public void testReplaceCatchClauseWithThrowsWithFinally() throws Exception {
	
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");	
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        try {\n");		
		buf.append("            goo();\n");
		buf.append("        } catch (IOException e) {\n");
		buf.append("        } finally {\n");
		buf.append("        }\n");	
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "(IOException e)";
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");		
		buf.append("public class E {\n");
		buf.append("    public void foo() throws IOException {\n");
		buf.append("        try {\n");		
		buf.append("            goo();\n");
		buf.append("        } finally {\n");
		buf.append("        }\n");	
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");		
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        try {\n");		
		buf.append("            goo();\n");
		buf.append("        } finally {\n");
		buf.append("        }\n");	
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
				
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });	

	}
	
	public void testReplaceSingleCatchClauseWithThrows() throws Exception {
	
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");	
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        try {\n");		
		buf.append("            goo();\n");
		buf.append("        } catch (IOException e) {\n");
		buf.append("        }\n");	
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "(IOException e)";
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str) + str.length(), 0);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");		
		buf.append("public class E {\n");
		buf.append("    public void foo() throws IOException {\n");
		buf.append("        goo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");		
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        goo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");	
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        try {\n");		
		buf.append("            goo();\n");
		buf.append("        } catch (IOException e) {\n");
		buf.append("        } finally {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();
				
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });	

	}
	
	public void testUnwrapForLoop() throws Exception {
	
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (int i= 0; i < 3; i++) {\n");		
		buf.append("            goo();\n");
		buf.append("        }\n");	
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "for";
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str) + str.length(), 0);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        goo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());	
	}
	
	public void testUnwrapDoStatement() throws Exception {
	
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        do {\n");		
		buf.append("            goo();\n");
		buf.append("            goo();\n");
		buf.append("            goo();\n");
		buf.append("        } while (true);\n");	
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "do";
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str) + str.length(), 0);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        goo();\n");
		buf.append("        goo();\n");
		buf.append("        goo();\n");		
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());	
	}	
	
	public void testUnwrapWhileLoop2Statements() throws Exception {
	
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        while (true) {\n");		
		buf.append("            goo();\n");
		buf.append("            System.out.println();\n");		
		buf.append("        }\n");	
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "while";
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str) + str.length(), 0);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        goo();\n");
		buf.append("        System.out.println();\n");		
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());	
	}
	
	public void testUnwrapIfStatement() throws Exception {
	
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (1+ 3 == 6) {\n");		
		buf.append("            StringBuffer buf= new StringBuffer();\n");
		buf.append("            buf.append(1);\n");
		buf.append("            buf.append(2);\n");
		buf.append("            buf.append(3);\n");
		buf.append("        }\n");	
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "if";
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str) + str.length(), 0);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        StringBuffer buf= new StringBuffer();\n");
		buf.append("        buf.append(1);\n");
		buf.append("        buf.append(2);\n");
		buf.append("        buf.append(3);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (1+ 3 == 6) {\n");		
		buf.append("            StringBuffer buf= new StringBuffer();\n");
		buf.append("            buf.append(1);\n");
		buf.append("            buf.append(2);\n");
		buf.append("            buf.append(3);\n");
		buf.append("        } else {\n");
		buf.append("        }\n");	
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
				
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });	
	}
	
	public void testUnwrapTryStatement() throws Exception {
	
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        try {\n");		
		buf.append("            StringBuffer buf= new StringBuffer();\n");
		buf.append("            buf.append(1);\n");
		buf.append("            buf.append(2);\n");
		buf.append("            buf.append(3);\n");
		buf.append("        } finally {\n");
		buf.append("            return;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "try";
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str) + str.length(), 0);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        StringBuffer buf= new StringBuffer();\n");
		buf.append("        buf.append(1);\n");
		buf.append("        buf.append(2);\n");
		buf.append("        buf.append(3);\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());	
	}
	
	public void testUnwrapAnonymous() throws Exception {
	
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        Runnable run= new Runnable() {\n");
		buf.append("            public void run() { \n");
		buf.append("                throw new NullPointerException();\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "};";
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        throw new NullPointerException();\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());	
	}
	
	public void testUnwrapBlock() throws Exception {
	
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        {\n");
		buf.append("            { \n");
		buf.append("                throw new NullPointerException();\n");
		buf.append("            }//comment\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "}//comment";
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        {\n");
		buf.append("            throw new NullPointerException();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());	
	}
	
	
	public void testUnwrapMethodInvocation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public int foo() {\n");
		buf.append("        return Math.abs(9+ 8);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "Math.abs(9+ 8)";
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str) + str.length(), 0);
		List proposals= collectAssists(context, false);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public int foo() {\n");
		buf.append("        return 9+ 8;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		assertExpectedExistInProposals(proposals, new String[] {expected1});
	}
	
	public void testSplitDeclaration1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i = 9;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "=";
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i;\n");
		buf.append("        i = 9;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());	
	}
	
	public void testSplitDeclaration2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (int i = 0; i < 9; i++) {\n");
		buf.append("       }\n");		
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "=";
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i;\n");
		buf.append("        for (i = 0; i < 9; i++) {\n");
		buf.append("       }\n");		
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());	
	}
	
	public void testSplitDeclaration3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        final int i[] = null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "i[]";
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        final int i[];\n");
		buf.append("        i = null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());	
	}	
	
	public void testJoinDeclaration1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int var[];\n");
		buf.append("        foo();\n");
		buf.append("        var = null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "var[]";
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int var[] = null;\n");
		buf.append("        foo();\n");
		buf.append("    }\n");
		buf.append("}\n");		


		assertEqualString(preview, buf.toString());	
	}
	
	
	public void testJoinDeclaration2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int var[];\n");
		buf.append("        foo();\n");
		buf.append("        var = null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "var = ";
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
		List proposals= collectAssists(context, false);
		
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        foo();\n");
		buf.append("        int var[] = null;\n");
		buf.append("    }\n");
		buf.append("}\n");		
		String expected1= buf.toString();
		
		assertExpectedExistInProposals(proposals, new String[] {expected1});
	}
	
	private static final Class[] FILTER_EQ= { LinkedNamesAssistProposal.class, AssignToVariableAssistProposal.class };
	
    public void testInvertEquals() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        StringBuffer buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        \"a\".equals(\"b\");\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
        
        String str= "equals";
        AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
        List proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);
        
        CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
        String preview= getPreviewContent(proposal);

        buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        \"b\".equals(\"a\");\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
        
        cu= pack1.createCompilationUnit("E.java", buf.toString(), true, null);
        context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);
        
        proposal= (CUCorrectionProposal) proposals.get(0);
        preview= getPreviewContent(proposal);

        buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        \"a\".equals(\"b\");\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }
    
    public void testInvertEquals2() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        StringBuffer buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        String s= \"a\";\n");
        buf.append("        s.equals(\"a\");\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
        
        String str= "equals";
        AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
        List proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);
        
        CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
        String preview= getPreviewContent(proposal);

        buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        String s= \"a\";\n");
        buf.append("        \"a\".equals(s);\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
        
        cu= pack1.createCompilationUnit("E.java", buf.toString(), true, null);
        context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);
        
        proposal= (CUCorrectionProposal) proposals.get(0);
        preview= getPreviewContent(proposal);

        buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        String s= \"a\";\n");
        buf.append("        s.equals(\"a\");\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }
    
    public void testInvertEquals3() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        StringBuffer buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    private String a= \"a\";\n");
        buf.append("    private String b= \"b\";\n");
        buf.append("    public void foo() {\n");
        buf.append("        a.equals(b);\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
        
        String str= "equals";
        AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
        List proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);
        
        CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
        String preview= getPreviewContent(proposal);

        buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    private String a= \"a\";\n");
        buf.append("    private String b= \"b\";\n");
        buf.append("    public void foo() {\n");
        buf.append("        b.equals(a);\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
        
        cu= pack1.createCompilationUnit("E.java", buf.toString(), true, null);
        context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);
        
        proposal= (CUCorrectionProposal) proposals.get(0);
        preview= getPreviewContent(proposal);

        buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    private String a= \"a\";\n");
        buf.append("    private String b= \"b\";\n");
        buf.append("    public void foo() {\n");
        buf.append("        a.equals(b);\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }
    
    public void testInvertEquals4() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        StringBuffer buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("class S {\n");
        buf.append("    protected String sup= \"a\";\n");
        buf.append("}\n");
        buf.append("public class E extends S {\n");
        buf.append("    private String a= \"a\";\n");
        buf.append("    public void foo() {\n");
        buf.append("        sup.equals(this.a);\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
        
        String str= "equals";
        AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
        List proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);
        
        CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
        String preview= getPreviewContent(proposal);

        buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("class S {\n");
        buf.append("    protected String sup= \"a\";\n");
        buf.append("}\n");
        buf.append("public class E extends S {\n");
        buf.append("    private String a= \"a\";\n");
        buf.append("    public void foo() {\n");
        buf.append("        this.a.equals(sup);\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
        
        cu= pack1.createCompilationUnit("E.java", buf.toString(), true, null);
        context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);
        
        proposal= (CUCorrectionProposal) proposals.get(0);
        preview= getPreviewContent(proposal);

        buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("class S {\n");
        buf.append("    protected String sup= \"a\";\n");
        buf.append("}\n");
        buf.append("public class E extends S {\n");
        buf.append("    private String a= \"a\";\n");
        buf.append("    public void foo() {\n");
        buf.append("        sup.equals(this.a);\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    public void testInvertEquals5() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        StringBuffer buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("class A {\n");
        buf.append("    static String A= \"a\";\n");
        buf.append("}\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        \"a\".equals(A.A);\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
        
        String str= "equals";
        AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
        List proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);
        
        CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
        String preview= getPreviewContent(proposal);

        buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("class A {\n");
        buf.append("    static String A= \"a\";\n");
        buf.append("}\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        A.A.equals(\"a\");\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
        
        cu= pack1.createCompilationUnit("E.java", buf.toString(), true, null);
        context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);
        
        proposal= (CUCorrectionProposal) proposals.get(0);
        preview= getPreviewContent(proposal);

        buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("class A {\n");
        buf.append("    static String A= \"a\";\n");
        buf.append("}\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        \"a\".equals(A.A);\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    public void testInvertEquals6() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        StringBuffer buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("class A {\n");
        buf.append("    static String get() {\n");
        buf.append("        return \"a\";\n");
        buf.append("    }\n");
        buf.append("}\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        \"a\".equals(A.get());\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
        
        String str= "equals";
        AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
        List proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);
        
        cu= pack1.createCompilationUnit("E.java", buf.toString(), true, null);
        CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
        String preview= getPreviewContent(proposal);

        buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("class A {\n");
        buf.append("    static String get() {\n");
        buf.append("        return \"a\";\n");
        buf.append("    }\n");
        buf.append("}\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        A.get().equals(\"a\");\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
        
        cu= pack1.createCompilationUnit("E.java", buf.toString(), true, null);
        context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);
        
        proposal= (CUCorrectionProposal) proposals.get(0);
        preview= getPreviewContent(proposal);

        buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("class A {\n");
        buf.append("    static String get() {\n");
        buf.append("        return \"a\";\n");
        buf.append("    }\n");
        buf.append("}\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        \"a\".equals(A.get());\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    public void testInvertEquals7() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        StringBuffer buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        \"a\".getClass().equals(String.class);\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
        
        String str= "equals";
        AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
        List proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);
        
        CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
        String preview= getPreviewContent(proposal);

        buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        String.class.equals(\"a\".getClass());\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
        
        cu= pack1.createCompilationUnit("E.java", buf.toString(), true, null);
        context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);
        
        proposal= (CUCorrectionProposal) proposals.get(0);
        preview= getPreviewContent(proposal);

        buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        \"a\".getClass().equals(String.class);\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    public void testInvertEquals8() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        StringBuffer buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        boolean x = false && \"a\".equals(get());\n");
        buf.append("    }\n");
        buf.append("    String get() {\n");
        buf.append("        return \"a\";\n");
        buf.append("    }\n");
        buf.append("}\n");
        
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
        
        String str= "equals";
        AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
        List proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);
        
        CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
        String preview= getPreviewContent(proposal);

        buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        boolean x = false && get().equals(\"a\");\n");
        buf.append("    }\n");
        buf.append("    String get() {\n");
        buf.append("        return \"a\";\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());

        cu= pack1.createCompilationUnit("E.java", buf.toString(), true, null);
        context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);
        
        proposal= (CUCorrectionProposal) proposals.get(0);
        preview= getPreviewContent(proposal);

        buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        boolean x = false && \"a\".equals(get());\n");
        buf.append("    }\n");
        buf.append("    String get() {\n");
        buf.append("        return \"a\";\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }

    public void testInvertEquals9() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        StringBuffer buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        equals(new E());\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
        
        String str= "equals";
        AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
        List proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);
        
        CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
        String preview= getPreviewContent(proposal);

        buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        new E().equals(this);\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
        
        cu= pack1.createCompilationUnit("E.java", buf.toString(), true, null);
        context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);
        
        proposal= (CUCorrectionProposal) proposals.get(0);
        preview= getPreviewContent(proposal);

        buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        equals(new E());\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }
    
    public void testInvertEquals10() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        StringBuffer buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        \"a\".equals(null);\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
        
        String str= "equals";
        AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
        List proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 0);
        assertCorrectLabels(proposals);
    }
    
    public void testInvertEquals11() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        StringBuffer buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    boolean equals(Object o, boolean a) {\n");
        buf.append("        return false;\n");
        buf.append("    }\n");  
        buf.append("    public void foo() {\n");        
        buf.append("        new E().equals(\"a\", false);\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
        
        String str= "E().equals";
        AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str) + str.length(), 0);
        List proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 0);
        assertCorrectLabels(proposals);
        
        context= getCorrectionContext(cu, buf.toString().lastIndexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 0);
        assertCorrectLabels(proposals);
    }
    
    public void testInvertEquals12() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        StringBuffer buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    boolean equals(boolean b) {\n");
        buf.append("        return false;\n");
        buf.append("    }\n");  
        buf.append("    public void foo() {\n");        
        buf.append("        new E().equals(false);\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
        
        String str= "E().equals";
        AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str) + str.length(), 0);
        List proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 0);
        assertCorrectLabels(proposals);
        
        context= getCorrectionContext(cu, buf.toString().lastIndexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 0);
        assertCorrectLabels(proposals);
    }
    
    public void testInvertEquals13() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        StringBuffer buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    boolean equals(boolean b) {\n");
        buf.append("        return false;\n");
        buf.append("    }\n");  
        buf.append("    public void foo() {\n");        
        buf.append("        new E().equals(true ? true : false);\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
        
        String str= "E().equals";
        AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str) + str.length(), 0);
        List proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 0);
        assertCorrectLabels(proposals);
        
        context= getCorrectionContext(cu, buf.toString().lastIndexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 0);
        assertCorrectLabels(proposals);
    }
    
    public void testInvertEquals14() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        StringBuffer buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("class Super {\n");
        buf.append("    protected boolean sBool= false;\n");
        buf.append("}\n");
        buf.append("public class E extends Super {\n");
        buf.append("    boolean equals(boolean b) {\n");
        buf.append("        return false;\n");
        buf.append("    }\n");  
        buf.append("    public void foo() {\n");        
        buf.append("        new E().equals(sBool);\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
        
        String str= "E().equals";
        AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str) + str.length(), 0);
        List proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 0);
        assertCorrectLabels(proposals);
        
        context= getCorrectionContext(cu, buf.toString().lastIndexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 0);
        assertCorrectLabels(proposals);
    }
    
    public void testInvertEquals15() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        StringBuffer buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    boolean equals(int i) {\n");
        buf.append("        return false;\n");
        buf.append("    }\n");  
        buf.append("    public void foo() {\n");        
        buf.append("        new E().equals(1 + 1);\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
        
        String str= "E().equals";
        AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str) + str.length(), 0);
        List proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 0);
        assertCorrectLabels(proposals);
        
        context= getCorrectionContext(cu, buf.toString().lastIndexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 0);
        assertCorrectLabels(proposals);
    }
    
    public void testInvertEquals16() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        StringBuffer buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    boolean equals(int i) {\n");
        buf.append("        return false;\n");
        buf.append("    }\n");  
        buf.append("    public void foo() {\n");
        buf.append("        int i= 1;\n");
        buf.append("        new E().equals(i + i);\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
        
        String str= "E().equals";
        AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str) + str.length(), 0);
        List proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 0);
        assertCorrectLabels(proposals);
        
        context= getCorrectionContext(cu, buf.toString().lastIndexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 0);
        assertCorrectLabels(proposals);
    }
    
    public void testInvertEquals17() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        StringBuffer buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("       \"a\".equals(null);\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
        
        String str= "equals";
        AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
        List proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 0);
        assertCorrectLabels(proposals);
    }
    
    public void testInvertEquals18() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        StringBuffer buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public boolean equals(Object o) {\n");
        buf.append("       return super.equals(o);\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
        
        String str= "equals(o)";
        AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
        List proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 0);
        assertCorrectLabels(proposals);
        
        context= getCorrectionContext(cu, buf.toString().lastIndexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 0);
        assertCorrectLabels(proposals);
    }
    
    public void testInvertEquals19() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        StringBuffer buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    private String a= \"a\";\n");
        buf.append("    public void foo() {\n");
        buf.append("        a.equals((Object) \"a\");\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
        
        String str= "equals";
        AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
        List proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);
        
        CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
        String preview= getPreviewContent(proposal);

        buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    private String a= \"a\";\n");        
        buf.append("    public void foo() {\n");
        buf.append("        ((Object) \"a\").equals(a);\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
        
        cu= pack1.createCompilationUnit("E.java", buf.toString(), true, null);
        context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);
        
        proposal= (CUCorrectionProposal) proposals.get(0);
        preview= getPreviewContent(proposal);

        buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    private String a= \"a\";\n");        
        buf.append("    public void foo() {\n");
        buf.append("        a.equals((Object) \"a\");\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }
    
    public void testInvertEquals20() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        StringBuffer buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        String s= null;\n");
        buf.append("        \"a\".equals(s = \"a\");\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
        
        String str= "equals";
        AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
        List proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);
        
        CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
        String preview= getPreviewContent(proposal);

        buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        String s= null;\n");
        buf.append("        (s = \"a\").equals(\"a\");\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
        
        cu= pack1.createCompilationUnit("E.java", buf.toString(), true, null);
        context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);
        
        proposal= (CUCorrectionProposal) proposals.get(0);
        preview= getPreviewContent(proposal);

        buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        String s= null;\n");
        buf.append("        \"a\".equals(s = \"a\");\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }
    
    public void testInvertEquals21() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        StringBuffer buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        \"aaa\".equals(\"a\" + \"a\" + \"a\");\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
        
        String str= "equals";
        AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
        List proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);
        
        CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
        String preview= getPreviewContent(proposal);

        buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        (\"a\" + \"a\" + \"a\").equals(\"aaa\");\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
        
        cu= pack1.createCompilationUnit("E.java", buf.toString(), true, null);
        context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);
        
        proposal= (CUCorrectionProposal) proposals.get(0);
        preview= getPreviewContent(proposal);

        buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        \"aaa\".equals(\"a\" + \"a\" + \"a\");\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }
    
    public void testInvertEquals22() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        StringBuffer buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        \"a\".equals(true ? \"a\" : \"b\");\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
        
        String str= "equals";
        AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
        List proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);
        
        CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
        String preview= getPreviewContent(proposal);

        buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        (true ? \"a\" : \"b\").equals(\"a\");\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
        
        cu= pack1.createCompilationUnit("E.java", buf.toString(), true, null);
        context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);
        
        proposal= (CUCorrectionProposal) proposals.get(0);
        preview= getPreviewContent(proposal);

        buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        \"a\".equals(true ? \"a\" : \"b\");\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }
    
    public void testInvertEquals23() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        StringBuffer buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        \"a\".equals((\"a\"));\n");
        buf.append("    }\n");
        buf.append("}\n");
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
        
        String str= "equals";
        AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
        List proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);
        
        CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
        String preview= getPreviewContent(proposal);

        buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        (\"a\").equals(\"a\");\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
        
        cu= pack1.createCompilationUnit("E.java", buf.toString(), true, null);
        context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);
        
        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);
        
        proposal= (CUCorrectionProposal) proposals.get(0);
        preview= getPreviewContent(proposal);

        buf= new StringBuffer();
        buf.append("package test1;\n");
        buf.append("public class E {\n");
        buf.append("    public void foo() {\n");
        buf.append("        \"a\".equals(\"a\");\n");
        buf.append("    }\n");
        buf.append("}\n");
        assertEqualString(preview, buf.toString());
    }
    
    
	public void testAddTypeToArrayInitializer() throws Exception {
		
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			StringBuffer buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("public class E {\n");
			buf.append("    public void foo() {\n");
			buf.append("        int[][] numbers= {{ 1, 2 }, { 3, 4 }, { 4, 5 }};\n");		
			buf.append("    }\n");
			buf.append("}\n");
			ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
			
			String str= "{{";
			AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str) + str.length(), 0);
			List proposals= collectAssists(context, false);
			
			assertNumberOfProposals(proposals, 1);
			assertCorrectLabels(proposals);
			
			CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
			String preview= getPreviewContent(proposal);

			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("public class E {\n");
			buf.append("    public void foo() {\n");
			buf.append("        int[][] numbers= new int[][]{{ 1, 2 }, { 3, 4 }, { 4, 5 }};\n");		
			buf.append("    }\n");
			buf.append("}\n");
			assertEqualString(preview, buf.toString());	
		}
	
	public void testCreateInSuper() throws Exception {
		
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			StringBuffer buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("public class A {\n");
			buf.append("}\n");
			pack1.createCompilationUnit("A.java", buf.toString(), false, null);
			
			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("public interface IB {\n");
			buf.append("}\n");
			pack1.createCompilationUnit("IB.java", buf.toString(), false, null);
			
			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("import java.io.IOException;\n");
			buf.append("import java.util.Vector;\n");
			buf.append("public class E extends A implements IB {\n");
			buf.append("    public Vector foo(int count) throws IOException {\n");
			buf.append("        return null;\n");
			buf.append("    }\n");
			buf.append("}\n");
			ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
			
			String str= "foo";
			AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str) + str.length(), 0);
			List proposals= collectAssists(context, false);
			
			assertNumberOfProposals(proposals, 2);
			assertCorrectLabels(proposals);
			
			CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
			String preview1= getPreviewContent(proposal);

			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("\n");
			buf.append("import java.io.IOException;\n");
			buf.append("import java.util.Vector;\n");
			buf.append("\n");
			buf.append("public interface IB {\n");
			buf.append("\n");
			buf.append("    Vector foo(int count) throws IOException;\n");
			buf.append("}\n");
			String expected1= buf.toString();
			
			proposal= (CUCorrectionProposal) proposals.get(1);
			String preview2= getPreviewContent(proposal);

			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("\n");
			buf.append("import java.io.IOException;\n");
			buf.append("import java.util.Vector;\n");
			buf.append("\n");
			buf.append("public class A {\n");
			buf.append("\n");
			buf.append("    public Vector foo(int count) throws IOException {\n");
			buf.append("        //TODO\n");
			buf.append("        return null;\n");
			buf.append("    }\n");
			buf.append("}\n");
			String expected2= buf.toString();
					
			assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });	

		}
	
	public void testCreateInSuperInGeneric() throws Exception {
		
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			StringBuffer buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("public class A<T> {\n");
			buf.append("}\n");
			pack1.createCompilationUnit("A.java", buf.toString(), false, null);
			
			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("public interface IB<T> {\n");
			buf.append("}\n");
			pack1.createCompilationUnit("IB.java", buf.toString(), false, null);
			
			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("import java.io.IOException;\n");
			buf.append("import java.util.Vector;\n");
			buf.append("public class E extends A<String> implements IB<String> {\n");
			buf.append("    public Vector<String> foo(int count) throws IOException {\n");
			buf.append("        return null;\n");
			buf.append("    }\n");
			buf.append("}\n");
			ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
			
			String str= "foo";
			AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str) + str.length(), 0);
			List proposals= collectAssists(context, false);
			
			assertNumberOfProposals(proposals, 2);
			assertCorrectLabels(proposals);
			
			CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
			String preview1= getPreviewContent(proposal);

			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("\n");
			buf.append("import java.io.IOException;\n");
			buf.append("import java.util.Vector;\n");
			buf.append("\n");
			buf.append("public interface IB<T> {\n");
			buf.append("\n");
			buf.append("    Vector<String> foo(int count) throws IOException;\n");
			buf.append("}\n");
			String expected1= buf.toString();
			
			proposal= (CUCorrectionProposal) proposals.get(1);
			String preview2= getPreviewContent(proposal);

			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("\n");
			buf.append("import java.io.IOException;\n");
			buf.append("import java.util.Vector;\n");
			buf.append("\n");
			buf.append("public class A<T> {\n");
			buf.append("\n");
			buf.append("    public Vector<String> foo(int count) throws IOException {\n");
			buf.append("        //TODO\n");
			buf.append("        return null;\n");
			buf.append("    }\n");
			buf.append("}\n");
			String expected2= buf.toString();
					
			assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });	

		}

	public void testChangeIfStatementToBlock() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true) \n");
		buf.append("            ;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "if (";
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true) {\n");
		buf.append("            ;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true) \n");
		buf.append("            ;\n");
		buf.append("        else {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        ;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();
		
		assertExpectedExistInProposals(proposals, new String[] {expected1, expected2, expected3});
	}    
	
	public void testChangeElseStatementToBlock() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true) {\n");
		buf.append("            ;;\n");
		buf.append("        } else\n");
		buf.append("            ;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "else";
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true) {\n");
		buf.append("            ;;\n");
		buf.append("        } else {\n");
		buf.append("            ;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (false)\n");
		buf.append("            ;\n");
		buf.append("        else {\n");
		buf.append("            ;;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertExpectedExistInProposals(proposals, new String[] {expected1, expected2});
	}   
	
	public void testChangeIfWithElseStatementToBlock() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true)\n");
		buf.append("            ;\n");
		buf.append("        else {\n");
		buf.append("            ;;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "if (";
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true) {\n");
		buf.append("            ;\n");
		buf.append("        } else {\n");
		buf.append("            ;;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (false) {\n");
		buf.append("            ;;\n");
		buf.append("        } else\n");
		buf.append("            ;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertExpectedExistInProposals(proposals, new String[] {expected1, expected2});
	}   
	
	public void testChangeIfAndElseStatementToBlock1() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true)\n");
		buf.append("            ;\n");
		buf.append("        else\n");
		buf.append("            ;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "if (";
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true) {\n");
		buf.append("            ;\n");
		buf.append("        } else\n");
		buf.append("            ;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true) {\n");
		buf.append("            ;\n");
		buf.append("        } else {\n");
		buf.append("            ;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (false)\n");
		buf.append("            ;\n");
		buf.append("        else\n");
		buf.append("            ;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();
		
		assertExpectedExistInProposals(proposals, new String[] {expected1, expected2, expected3});
	}   
	
	public void testChangeIfAndElseStatementToBlock2() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true)\n");
		buf.append("            ;\n");
		buf.append("        else\n");
		buf.append("            ;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "else";
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true)\n");
		buf.append("            ;\n");
		buf.append("        else {\n");
		buf.append("            ;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true) {\n");
		buf.append("            ;\n");
		buf.append("        } else {\n");
		buf.append("            ;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (false)\n");
		buf.append("            ;\n");
		buf.append("        else\n");
		buf.append("            ;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();
		
		assertExpectedExistInProposals(proposals, new String[] {expected1, expected2, expected3});
	}   
	
	public void testChangeIfAndElseIfStatementToBlock() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true)\n");
		buf.append("            ;\n");
		buf.append("        else if (true)\n");
		buf.append("            ;\n");
		buf.append("        else if (false)\n");
		buf.append("            ;\n");
		buf.append("        else\n");
		buf.append("            ;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "else if (";
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true)\n");
		buf.append("            ;\n");
		buf.append("        else {\n");
		buf.append("            if (true)\n");
		buf.append("                ;\n");
		buf.append("            else if (false)\n");
		buf.append("                ;\n");
		buf.append("            else\n");
		buf.append("                ;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true) {\n");
		buf.append("            ;\n");
		buf.append("        } else if (true) {\n");
		buf.append("            ;\n");
		buf.append("        } else if (false) {\n");
		buf.append("            ;\n");
		buf.append("        } else {\n");
		buf.append("            ;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (false) {\n");
		buf.append("            if (true)\n");
		buf.append("                ;\n");
		buf.append("            else if (false)\n");
		buf.append("                ;\n");
		buf.append("            else\n");
		buf.append("                ;\n");
		buf.append("        } else\n");
		buf.append("            ;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();
		
		assertExpectedExistInProposals(proposals, new String[] {expected1, expected2, expected3});
	}   
	
	public void testChangeIfAndElseIfStatementWithBlockToBlock() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true)\n");
		buf.append("            ;\n");
		buf.append("        else if (true) {\n");
		buf.append("            ;\n");
		buf.append("        } else if (false)\n");
		buf.append("            ;\n");
		buf.append("        else\n");
		buf.append("            ;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "else if (";
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true)\n");
		buf.append("            ;\n");
		buf.append("        else {\n");
		buf.append("            if (true) {\n");
		buf.append("                ;\n");
		buf.append("            } else if (false)\n");
		buf.append("                ;\n");
		buf.append("            else\n");
		buf.append("                ;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true) {\n");
		buf.append("            ;\n");
		buf.append("        } else if (true) {\n");
		buf.append("            ;\n");
		buf.append("        } else if (false) {\n");
		buf.append("            ;\n");
		buf.append("        } else {\n");
		buf.append("            ;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (false) {\n");
		buf.append("            if (true) {\n");
		buf.append("                ;\n");
		buf.append("            } else if (false)\n");
		buf.append("                ;\n");
		buf.append("            else\n");
		buf.append("                ;\n");
		buf.append("        } else\n");
		buf.append("            ;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();
		
		assertExpectedExistInProposals(proposals, new String[] {expected1, expected2, expected3});
	}   
	
	public void testRemoveIfBlock01() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true) {\n");
		buf.append("            ;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "if (";
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true)\n");
		buf.append("            ;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true) {\n");
		buf.append("            ;\n");
		buf.append("        } else {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        ;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();
		
		assertExpectedExistInProposals(proposals, new String[] {expected1, expected2, expected3});
	}   
	
	public void testRemoveIfBlock02() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true) {\n");
		buf.append("            ;\n");
		buf.append("        } else {\n");
		buf.append("            ;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "if (";
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true) {\n");
		buf.append("            ;\n");
		buf.append("        } else\n");
		buf.append("            ;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true)\n");
		buf.append("            ;\n");
		buf.append("        else {\n");
		buf.append("            ;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true)\n");
		buf.append("            ;\n");
		buf.append("        else\n");
		buf.append("            ;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (false) {\n");
		buf.append("            ;\n");
		buf.append("        } else {\n");
		buf.append("            ;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected4= buf.toString();
		
		assertExpectedExistInProposals(proposals, new String[] {expected1, expected2, expected3, expected4});
	}   
	
	public void testRemoveIfBlock03() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true) {\n");
		buf.append("            ;\n");
		buf.append("        } else {\n");
		buf.append("            ;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "{\n            ;";
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true) {\n");
		buf.append("            ;\n");
		buf.append("        } else\n");
		buf.append("            ;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true)\n");
		buf.append("            ;\n");
		buf.append("        else {\n");
		buf.append("            ;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true)\n");
		buf.append("            ;\n");
		buf.append("        else\n");
		buf.append("            ;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();
		
		assertExpectedExistInProposals(proposals, new String[] {expected1, expected2, expected3});
	}   
	
	public void testRemoveWhileBlock01() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        while (true) {\n");
		buf.append("            ;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "while (";
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        while (true);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        ;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertExpectedExistInProposals(proposals, new String[] {expected1, expected2});
	}   
	
	public void testRemoveForBlock01() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (;;) {\n");
		buf.append("            ;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "for (";
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (;;);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        ;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertExpectedExistInProposals(proposals, new String[] {expected1, expected2});
	}   
	
	public void testRemoveDoBlock01() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        do {\n");
		buf.append("            ;\n");
		buf.append("        } while (true);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "do {";
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        do; while (true);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        ;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertExpectedExistInProposals(proposals, new String[] {expected1, expected2});
	}   
	
	public void testSurroundWithRunnable1() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(1);\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		StringBuffer selection= new StringBuffer();
		selection.append("        System.out.println(1);\n");
		
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 9);
		assertCorrectLabels(proposals);

		StringBuffer expected1= new StringBuffer();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                System.out.println(1);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		expected1.append("\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}   
	
	public void testSurroundWithRunnable2() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i = 10;\n");
		buf.append("        final int j = 10;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(j);\n");
		buf.append("        int k = 10;\n");
		buf.append("        k++;\n");
		buf.append("        System.out.println(k);\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(j);\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		StringBuffer selection= new StringBuffer();
		selection.append("        System.out.println(i);\n");
		selection.append("        System.out.println(j);\n");
		selection.append("        int k = 10;\n");
		selection.append("        k++;\n");
		selection.append("        System.out.println(k);\n");
		
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 9);
		assertCorrectLabels(proposals);

		StringBuffer expected1= new StringBuffer();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        final int i = 10;\n");
		expected1.append("        final int j = 10;\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                System.out.println(i);\n");
		expected1.append("                System.out.println(j);\n");
		expected1.append("                int k = 10;\n");
		expected1.append("                k++;\n");
		expected1.append("                System.out.println(k);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("        System.out.println(i);\n");
		expected1.append("        System.out.println(j);\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		expected1.append("\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}   

	public void testSurroundWithRunnable3() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i = 10;\n");
		buf.append("        int k = 10;\n");
		buf.append("        k++;\n");
		buf.append("        int h = 10;\n");
		buf.append("        int j = 10;\n");
		buf.append("        j++;\n");
		buf.append("        System.out.println(k);\n");
		buf.append("        System.out.println(j);\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(h);\n");
		buf.append("        i++;\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		StringBuffer selection= new StringBuffer();
		selection.append("        j++;\n");
		selection.append("        System.out.println(k);\n");
		selection.append("        System.out.println(j);\n");
		selection.append("        System.out.println(i);\n");
		selection.append("        System.out.println(h);\n");
		
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 9);
		assertCorrectLabels(proposals);

		StringBuffer expected1= new StringBuffer();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        final int i = 10;\n");
		expected1.append("        final int k = 10;\n");
		expected1.append("        k++;\n");
		expected1.append("        final int h = 10;\n");
		expected1.append("        final int j = 10;\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                j++;\n");
		expected1.append("                System.out.println(k);\n");
		expected1.append("                System.out.println(j);\n");
		expected1.append("                System.out.println(i);\n");
		expected1.append("                System.out.println(h);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("        i++;\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		expected1.append("\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}   
	
	public void testSurroundWithRunnable4() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int j = 10;\n");
		buf.append("        while (j > 0) {\n");
		buf.append("            System.out.println(j);\n");
		buf.append("            j--;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		StringBuffer selection= new StringBuffer();
		selection.append("            System.out.println(j);\n");
		selection.append("            j--;\n");
		
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 10);
		assertCorrectLabels(proposals);

		StringBuffer expected1= new StringBuffer();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        final int j = 10;\n");
		expected1.append("        while (j > 0) {\n");
		expected1.append("            Runnable runnable = new Runnable() {\n");
		expected1.append("                public void run() {\n");
		expected1.append("                    System.out.println(j);\n");
		expected1.append("                    j--;\n");
		expected1.append("                }\n");
		expected1.append("            };\n");
		expected1.append("        }\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		
		StringBuffer expected2= new StringBuffer();
		expected2.append("package test1;\n");
		expected2.append("public class E {\n");
		expected2.append("    public void foo() {\n");
		expected2.append("        int j = 10;\n");
		expected2.append("        System.out.println(j);\n");
		expected2.append("        j--;\n");
		expected2.append("    }\n");
		expected2.append("}\n");
		
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString(), expected2.toString()});
	}
	
	public void testSurroundWithRunnable5() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i = 10;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		StringBuffer selection= new StringBuffer();
		selection.append("        int i = 10;\n");
		
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 10);
		assertCorrectLabels(proposals);

		StringBuffer expected1= new StringBuffer();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        int i;\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                i = 10;\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("        System.out.println(i);\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		expected1.append("\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}
	
	public void testSurroundWithRunnable6() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        /***/ int i = 10;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(i);\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		StringBuffer selection= new StringBuffer();
		selection.append("        /***/ int i = 10;\n");
		selection.append("        System.out.println(i);\n");
		
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 10);
		assertCorrectLabels(proposals);

		StringBuffer expected1= new StringBuffer();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        /***/ int i;\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                i = 10;\n");
		expected1.append("                System.out.println(i);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("        System.out.println(i);\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		expected1.append("\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}
	
	public void testSurroundWithRunnable7() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        /***/ int i = 10;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		StringBuffer selection= new StringBuffer();
		selection.append("        System.out.println(i);\n");
		
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 9);
		assertCorrectLabels(proposals);

		StringBuffer expected1= new StringBuffer();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        /***/ final int i = 10;\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                System.out.println(i);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		expected1.append("\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}
	
	public void testSurroundWithRunnable8() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        //TextTextText\n");
		buf.append("        \n");
		buf.append("        //TextTextText\n");
		buf.append("        //\n");
		buf.append("        //TextTextText\n");
		buf.append("        /***/ int i = 10;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		StringBuffer selection= new StringBuffer();
		selection.append("int i = 10;\n");
		selection.append("        System.out.println(i);\n");
		
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 10);
		assertCorrectLabels(proposals);

		StringBuffer expected1= new StringBuffer();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        //TextTextText\n");
		expected1.append("        \n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                //TextTextText\n");
		expected1.append("                //\n");
		expected1.append("                //TextTextText\n");
		expected1.append("                /***/ int i = 10;\n");
		expected1.append("                System.out.println(i);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		expected1.append("\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}
	
	public void testSurroundWithRunnable9() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        /***/ int i = 10;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(i);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		StringBuffer selection= new StringBuffer();
		selection.append("int i = 10;\n");
		selection.append("        System.out.println(i);\n");
		
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 10);
		assertCorrectLabels(proposals);

		StringBuffer expected1= new StringBuffer();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        /***/ int i;\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                i = 10;\n");
		expected1.append("                System.out.println(i);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("        System.out.println(i);\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}
	
	public void testSurroundWithRunnable10() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i = 10;\n");
		buf.append("        int j;\n");
		buf.append("        System.out.println(10);\n");
		buf.append("        j = 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		StringBuffer selection= new StringBuffer();
		selection.append("        int i = 10;\n");
		selection.append("        int j;\n");
		selection.append("        System.out.println(10);\n");
		
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 10);
		assertCorrectLabels(proposals);

		StringBuffer expected1= new StringBuffer();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        int j;\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                int i = 10;\n");
		expected1.append("                System.out.println(10);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("        j = 10;\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		expected1.append("\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});	
	}
	
	public void testSurroundWithRunnable11() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(i);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		StringBuffer selection= new StringBuffer();
		selection.append("        int i;\n");
		selection.append("        System.out.println(i);\n");
		
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 10);
		assertCorrectLabels(proposals);

		StringBuffer expected1= new StringBuffer();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        final int i;\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                System.out.println(i);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("        System.out.println(i);\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});	
	}
	
	public void testSurroundWithRunnable12() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int i, String s) {\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(s);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		StringBuffer selection= new StringBuffer();
		selection.append("        System.out.println(i);\n");
		selection.append("        System.out.println(s);\n");
		
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 9);
		assertCorrectLabels(proposals);

		StringBuffer expected1= new StringBuffer();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo(final int i, final String s) {\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                System.out.println(i);\n");
		expected1.append("                System.out.println(s);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});	
	}
	
	public void testSurroundWithRunnable13() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int i, String s) {\n");
		buf.append("        i = 10;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(s);\n");
		buf.append("        s = \"\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		StringBuffer selection= new StringBuffer();
		selection.append("        System.out.println(i);\n");
		selection.append("        System.out.println(s);\n");
		
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 9);
		assertCorrectLabels(proposals);

		StringBuffer expected1= new StringBuffer();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo(final int i, final String s) {\n");
		expected1.append("        i = 10;\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                System.out.println(i);\n");
		expected1.append("                System.out.println(s);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("        s = \"\";\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});	
	}
	
	public void testSurroundWithRunnable14() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int j,i = 10;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(j);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		StringBuffer selection= new StringBuffer();
		selection.append("        System.out.println(i);\n");
		
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 9);
		assertCorrectLabels(proposals);

		StringBuffer expected1= new StringBuffer();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        int j;\n");
		expected1.append("        final int i = 10;\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                System.out.println(i);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("        System.out.println(j);\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});	
	}
	
	public void testSurroundWithRunnable15() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int j,i = 10;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        j = 10;\n");
		buf.append("        j++;\n");
		buf.append("        System.out.println(j);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		StringBuffer selection= new StringBuffer();
		selection.append("        System.out.println(i);\n");
		
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 9);
		assertCorrectLabels(proposals);

		StringBuffer expected1= new StringBuffer();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        int j;\n");
		expected1.append("        final int i = 10;\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                System.out.println(i);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("        j = 10;\n");
		expected1.append("        j++;\n");
		expected1.append("        System.out.println(j);\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});	
	}
	
	public void testSurroundWithRunnable16() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int j, i = 10;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(j);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		StringBuffer selection= new StringBuffer();
		selection.append("        int j, i = 10;\n");
		selection.append("        System.out.println(i);\n");
		
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 10);
		assertCorrectLabels(proposals);

		StringBuffer expected1= new StringBuffer();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        int j;\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                int i = 10;\n");
		expected1.append("                System.out.println(i);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("        System.out.println(j);\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});	
	}
	
	public void testSurroundWithRunnable17() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i = 10, j = i;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(j);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		StringBuffer selection= new StringBuffer();
		selection.append("        System.out.println(i);\n");
		
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 9);
		assertCorrectLabels(proposals);

		StringBuffer expected1= new StringBuffer();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        final int i = 10;\n");
		expected1.append("        int j = i;\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                System.out.println(i);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("        System.out.println(j);\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});	
	}
	
	public void testSurroundWithRunnable18() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i = 10, j = i;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(j);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		StringBuffer selection= new StringBuffer();
		selection.append("        System.out.println(i);\n");
		selection.append("        System.out.println(j);\n");
		
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 9);
		assertCorrectLabels(proposals);

		StringBuffer expected1= new StringBuffer();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        final int i = 10, j = i;\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                System.out.println(i);\n");
		expected1.append("                System.out.println(j);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});	
	}
	
	public void testSurroundWithRunnable19() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i = 10, k = i, j = k;\n");
		buf.append("        System.out.println(k);\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(j);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		StringBuffer selection= new StringBuffer();
		selection.append("        System.out.println(k);\n");
		
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 9);
		assertCorrectLabels(proposals);

		StringBuffer expected1= new StringBuffer();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        int i = 10;\n");
		expected1.append("        final int k = i;\n");
		expected1.append("        int j = k;\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                System.out.println(k);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("        System.out.println(i);\n");
		expected1.append("        System.out.println(j);\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});	
	}
	
	public void testSurroundWithRunnable20() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i = 10, j = 10;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(j);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		StringBuffer selection= new StringBuffer();
		selection.append("        int i = 10, j = 10;\n");
		selection.append("        System.out.println(i);\n");
		selection.append("        System.out.println(j);\n");
		
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 10);
		assertCorrectLabels(proposals);

		StringBuffer expected1= new StringBuffer();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                int i = 10, j = 10;\n");
		expected1.append("                System.out.println(i);\n");
		expected1.append("                System.out.println(j);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});	
	}
	
	public void testSurroundWithRunnable21() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true) {\n");
		buf.append("            int j = 10;\n");
		buf.append("            System.out.println(j);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		StringBuffer selection= new StringBuffer();
		selection.append("            int j = 10;\n");
		
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 11);
		assertCorrectLabels(proposals);

		StringBuffer expected1= new StringBuffer();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        if (true) {\n");
		expected1.append("            int j;\n");
		expected1.append("            Runnable runnable = new Runnable() {\n");
		expected1.append("                public void run() {\n");
		expected1.append("                    j = 10;\n");
		expected1.append("                }\n");
		expected1.append("            };\n");
		expected1.append("            System.out.println(j);\n");
		expected1.append("        }\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});	
	}
	
	public void testSurroundWithRunnable22() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true) {\n");
		buf.append("            int j = 10;\n");
		buf.append("            while (j == 10) {\n");
		buf.append("                System.out.println(j);\n");
		buf.append("                j--;\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		StringBuffer selection= new StringBuffer();
		selection.append("            while (j == 10) {\n");
		selection.append("                System.out.println(j);\n");
		selection.append("                j--;\n");
		selection.append("            }\n");
		
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 10);
		assertCorrectLabels(proposals);

		StringBuffer expected1= new StringBuffer();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        if (true) {\n");
		expected1.append("            final int j = 10;\n");
		expected1.append("            Runnable runnable = new Runnable() {\n");
		expected1.append("                public void run() {\n");
		expected1.append("                    while (j == 10) {\n");
		expected1.append("                        System.out.println(j);\n");
		expected1.append("                        j--;\n");
		expected1.append("                    }\n");
		expected1.append("                }\n");
		expected1.append("            };\n");
		expected1.append("        }\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});	
	}
	
	public void testSurroundWithRunnable23() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    int i= 9;\n");
		buf.append("    {\n");
		buf.append("        /***/ int k = 10;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		StringBuffer selection= new StringBuffer();
		selection.append("        /***/ int k = 10;\n");
		selection.append("        System.out.println(i);\n");
		
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 10);
		assertCorrectLabels(proposals);

		StringBuffer expected1= new StringBuffer();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    int i= 9;\n");
		expected1.append("    {\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                /***/ int k = 10;\n");
		expected1.append("                System.out.println(i);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});	
	}
	
	public void testSurroundWithRunnable24() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int k = 0, v = 0;\n");
		buf.append("        {\n");
		buf.append("            System.out.println(v);\n");
		buf.append("            System.out.println(k);\n");
		buf.append("        }\n");
		buf.append("        k++;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		StringBuffer selection= new StringBuffer();
		selection.append("            System.out.println(v);\n");
		selection.append("            System.out.println(k);\n");
		
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 10);
		assertCorrectLabels(proposals);

		StringBuffer expected1= new StringBuffer();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        final int k = 0, v = 0;\n");
		expected1.append("        {\n");
		expected1.append("            Runnable runnable = new Runnable() {\n");
		expected1.append("                public void run() {\n");
		expected1.append("                    System.out.println(v);\n");
		expected1.append("                    System.out.println(k);\n");
		expected1.append("                }\n");
		expected1.append("            };\n");
		expected1.append("        }\n");
		expected1.append("        k++;\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});	
	}
	/*
	public void testSurroundWithRunnable25() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int y = 1;\n");
		buf.append("        switch (y) {\n");
		buf.append("        case 1:\n");
		buf.append("            int e4 = 9, e5 = 0;\n");
		buf.append("            System.out.println(e4);\n");
		buf.append("            e5++;\n");
		buf.append("        default:\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		StringBuffer selection= new StringBuffer();
		selection.append("            System.out.println(e4);\n");
		
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 9);
		assertCorrectLabels(proposals);

		StringBuffer expected1= new StringBuffer();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        int y = 1;\n");
		expected1.append("        switch (y) {\n");
		expected1.append("        case 1:\n");
		expected1.append("            final int e4 = 9;\n");
		expected1.append("                int e5 = 0;\n");
		expected1.append("            Runnable runnable = new Runnable() {\n");
		expected1.append("                    public void run() {\n");
		expected1.append("                        System.out.println(e4);\n");
		expected1.append("                    }\n");
		expected1.append("                };\n");
		expected1.append("                e5++;\n");
		expected1.append("        default:\n");
		expected1.append("        }\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});	
	}
	
	public void testSurroundWithRunnable26() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int y = 1;\n");
		buf.append("        switch (y) {\n");
		buf.append("        case 1:\n");
		buf.append("            int e4 = 9, e5 = 0;\n");
		buf.append("            System.out.println(e4);\n");
		buf.append("            e5++;\n");
		buf.append("        default:\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		StringBuffer selection= new StringBuffer();
		selection.append("            int e4 = 9, e5 = 0;\n");
		selection.append("            System.out.println(e4);\n");
		
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 9);
		assertCorrectLabels(proposals);

		StringBuffer expected1= new StringBuffer();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        int y = 1;\n");
		expected1.append("        switch (y) {\n");
		expected1.append("        case 1:\n");
		expected1.append("            int e5;\n");
		expected1.append("                Runnable runnable = new Runnable() {\n");
		expected1.append("                    public void run() {\n");
		expected1.append("                        int e4 = 9;\n");
		expected1.append("                        e5 = 0;\n");
		expected1.append("                        System.out.println(e4);\n");
		expected1.append("                    }\n");
		expected1.append("                };\n");
		expected1.append("                e5++;\n");
		expected1.append("        default:\n");
		expected1.append("        }\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});	
	}*/
	
	public void testSurroundWithRunnable27() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        String s = \"\", c = \"\";\n");
		buf.append("        System.out.println(s);\n");
		buf.append("        c = \"\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		StringBuffer selection= new StringBuffer();
		selection.append("        System.out.println(s);\n");
		
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 9);
		assertCorrectLabels(proposals);

		StringBuffer expected1= new StringBuffer();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        final String s = \"\";\n");
		expected1.append("        String c = \"\";\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                System.out.println(s);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("        c = \"\";\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});	
	}
	
	public void testSurroundWithRunnable28() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i = 10, j, k, v;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(j);\n");
		buf.append("        System.out.println(v);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		StringBuffer selection= new StringBuffer();
		selection.append("        int i = 10, j, k, v;\n");
		selection.append("        System.out.println(i);\n");
		
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 10);
		assertCorrectLabels(proposals);

		StringBuffer expected1= new StringBuffer();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        int j;\n");
		expected1.append("        int v;\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                int i = 10;\n");
		expected1.append("                int k;\n");
		expected1.append("                System.out.println(i);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("        System.out.println(j);\n");
		expected1.append("        System.out.println(v);\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});	
	}
	
	public void testSurroundWithRunnable29() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        @SuppressWarnings(\"nls\") String s= \"\", k = \"\";\n");
		buf.append("        System.out.println(s);\n");
		buf.append("        System.out.println(k);\n");
		buf.append("        k=\"\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		StringBuffer selection= new StringBuffer();
		selection.append("        @SuppressWarnings(\"nls\") String s= \"\", k = \"\";\n");
		selection.append("        System.out.println(s);\n");
		selection.append("        System.out.println(k);\n");
		
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 10);
		assertCorrectLabels(proposals);

		StringBuffer expected1= new StringBuffer();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        @SuppressWarnings(\"nls\")\n");
		expected1.append("        String k;\n");
		expected1.append("        Runnable runnable = new Runnable() {\n");
		expected1.append("            public void run() {\n");
		expected1.append("                @SuppressWarnings(\"nls\") String s= \"\";\n");
		expected1.append("                k = \"\";\n");
		expected1.append("                System.out.println(s);\n");
		expected1.append("                System.out.println(k);\n");
		expected1.append("            }\n");
		expected1.append("        };\n");
		expected1.append("        k=\"\";\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});	
	}

	public void testSurroundWithRunnable30() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true) \n");
		buf.append("            System.out.println(1);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		StringBuffer selection= new StringBuffer();
		selection.append("            System.out.println(1);\n");
		
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 9);
		assertCorrectLabels(proposals);

		StringBuffer expected1= new StringBuffer();
		expected1.append("package test;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        if (true) {\n");
		expected1.append("            Runnable runnable = new Runnable() {\n");
		expected1.append("                public void run() {\n");
		expected1.append("                    System.out.println(1);\n");
		expected1.append("                }\n");
		expected1.append("            };\n");
		expected1.append("        }\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});	
	}
	
	public void testSurroundWithRunnableBug129876() throws Exception {
		Hashtable options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_UNNECESSARY_ELSE, JavaCore.WARNING);
		JavaCore.setOptions(options);
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (true) {\n");
		buf.append("            return;\n");
		buf.append("        } else {   \n");
		buf.append(";\n");
		buf.append("            return;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		StringBuffer selection= new StringBuffer();
		selection.append("{   \n");
		selection.append(";\n");
		selection.append("            return;\n");
		selection.append("        }");
		
		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 8);
		assertCorrectLabels(proposals);	
	}
	
	public void testMakeFinal01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    private int i= 0;\n");
		buf.append("    private void foo() {\n");
		buf.append("        System.out.println(i);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		int offset= buf.toString().indexOf("i=");
		AssistContext context= getCorrectionContext(cu, offset, 1);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    private final int i= 0;\n");
		buf.append("    private void foo() {\n");
		buf.append("        System.out.println(i);\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {buf.toString()});	
	}
	
	public void testMakeFinal02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    private final int i= 0;\n");
		buf.append("    private void foo() {\n");
		buf.append("        System.out.println(i);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		int offset= buf.toString().indexOf("i=");
		AssistContext context= getCorrectionContext(cu, offset, 1);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 0);
	}
	
	public void testMakeFinal03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public int i= 0;\n");
		buf.append("    private void foo() {\n");
		buf.append("        System.out.println(i);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		int offset= buf.toString().indexOf("i=");
		AssistContext context= getCorrectionContext(cu, offset, 1);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 0);
	}
	
	public void testMakeFinal04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    private void foo() {\n");
		buf.append("        int i= 0, j= 0;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(j);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		int offset= buf.toString().indexOf("i=");
		AssistContext context= getCorrectionContext(cu, offset, 1);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    private void foo() {\n");
		buf.append("        final int i= 0;\n");
		buf.append("        int j= 0;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(j);\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {buf.toString()});	
	}
	
	public void testMakeFinal05() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    private void foo(int i, int j) {\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(j);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		int offset= buf.toString().indexOf("i,");
		AssistContext context= getCorrectionContext(cu, offset, 1);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    private void foo(final int i, int j) {\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(j);\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {buf.toString()});	
	}

	public void testMakeFinal06() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i= 0;\n");
		buf.append("        i= 1;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		int offset= buf.toString().indexOf("i=");
		AssistContext context= getCorrectionContext(cu, offset, 1);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i;\n");
		buf.append("        i = 0;\n");
		buf.append("        i= 1;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {buf.toString()});
	}
	
	public void testMakeFinal07() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    private int i= 0;\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(i);\n");
		buf.append("    }\n");
		buf.append("    public void set(int i) {\n");
		buf.append("        this.i= i;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		int offset= buf.toString().indexOf("i=");
		AssistContext context= getCorrectionContext(cu, offset, 1);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 0);
	}
	
	public void testMakeFinal08() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    private int i= 0;\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(i);\n");
		buf.append("    }\n");
		buf.append("    public void reset() {\n");
		buf.append("        i= 0;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		int offset= buf.toString().indexOf("i=");
		AssistContext context= getCorrectionContext(cu, offset, 1);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 0);
	}
	
	public void testMakeFinal09() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    private int i= 0;\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(i);\n");
		buf.append("    }\n");
		buf.append("    public void reset() {\n");
		buf.append("        i--;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		int offset= buf.toString().indexOf("i=");
		AssistContext context= getCorrectionContext(cu, offset, 1);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 0);
	}
	
	public void testMakeFinal10() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    private int i= 0;\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(i);\n");
		buf.append("    }\n");
		buf.append("    public void reset() {\n");
		buf.append("        this.i++;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		int offset= buf.toString().indexOf("i=");
		AssistContext context= getCorrectionContext(cu, offset, 1);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 0);
	}
	
	public void testMakeFinal11() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (int j= 0, i= 0; j < (new int[0]).length; j++) {\n");
		buf.append("            System.out.println(i);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		int offset= buf.toString().indexOf("i=");
		AssistContext context= getCorrectionContext(cu, offset, 1);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
	}
	
	public void testMakeFinal12() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i= 1, j= i + 1, h= j + 1;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(j);\n");
		buf.append("        System.out.println(h);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		int offset= buf.toString().indexOf("i=");
		AssistContext context= getCorrectionContext(cu, offset, 1);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        final int i= 1;\n");
		buf.append("        int j= i + 1, h= j + 1;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(j);\n");
		buf.append("        System.out.println(h);\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {buf.toString()});	
	}
	
	public void testMakeFinal13() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i= 1, j= i + 1, h= j + 1;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(j);\n");
		buf.append("        System.out.println(h);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		int offset= buf.toString().indexOf("j=");
		AssistContext context= getCorrectionContext(cu, offset, 1);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i= 1;\n");
		buf.append("        final int j= i + 1;\n");
		buf.append("        int h= j + 1;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(j);\n");
		buf.append("        System.out.println(h);\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {buf.toString()});	
	}
	
	public void testMakeFinal14() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i= 1, j= i + 1, h= j + 1;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(j);\n");
		buf.append("        System.out.println(h);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		int offset= buf.toString().indexOf("h=");
		AssistContext context= getCorrectionContext(cu, offset, 1);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i= 1, j= i + 1;\n");
		buf.append("        final int h= j + 1;\n");
		buf.append("        System.out.println(i);\n");
		buf.append("        System.out.println(j);\n");
		buf.append("        System.out.println(h);\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {buf.toString()});	
	}
	
	public void testMakeFinal15() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("import java.io.Serializable;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        Serializable ser= new Serializable() {\n");
		buf.append("            private int i= 0;\n");
		buf.append("            Serializable ser2= new Serializable() {\n");
		buf.append("                public void foo() {\n");
		buf.append("                    System.out.println(i);\n");
		buf.append("                }\n");
		buf.append("            };\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		int offset= buf.toString().indexOf("i=");
		AssistContext context= getCorrectionContext(cu, offset, 1);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("import java.io.Serializable;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        Serializable ser= new Serializable() {\n");
		buf.append("            private final int i= 0;\n");
		buf.append("            Serializable ser2= new Serializable() {\n");
		buf.append("                public void foo() {\n");
		buf.append("                    System.out.println(i);\n");
		buf.append("                }\n");
		buf.append("            };\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {buf.toString()});	
	}
	
	public void testMakeFinal16() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i= 0;\n");
		buf.append("        Integer in= new Integer(i++);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		int offset= buf.toString().indexOf("i=");
		AssistContext context= getCorrectionContext(cu, offset, 1);
		List proposals= collectAssists(context, false);
		
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i;\n");
		buf.append("        i = 0;\n");
		buf.append("        Integer in= new Integer(i++);\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {buf.toString()});	
	}
	
}
