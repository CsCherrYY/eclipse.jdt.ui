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
package org.eclipse.jdt.internal.ui.refactoring;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jdt.core.JavaConventions;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;

import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.corext.refactoring.structure.ChangeSignatureRefactoring;

public class ParameterEditDialog extends StatusDialog {
	
	private ParameterInfo fParameter;
	private boolean fEditType;
	private Text fType;
	private Text fName;
	private Text fDefaultValue;

	public ParameterEditDialog(Shell parentShell, ParameterInfo parameter, boolean type) {
		super(parentShell);
		fParameter= parameter;
		fEditType= type;
	}
	
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		if (onlyNameEdit()) {
			newShell.setText(RefactoringMessages.getString("ParameterEditDialog.name.title")); //$NON-NLS-1$
		} else {
			newShell.setText(RefactoringMessages.getFormattedString("ParameterEditDialog.all.title", fParameter.getNewName())); //$NON-NLS-1$
		}
	}

	protected Control createDialogArea(Composite parent) {
		Composite result= (Composite)super.createDialogArea(parent);
		GridLayout layout= (GridLayout)result.getLayout();
		layout.numColumns= 2;
		Label label;
		GridData gd;
		if (fEditType) {
			label= new Label(result, SWT.NONE);
			label.setText(RefactoringMessages.getString("ParameterEditDialog.all.type")); //$NON-NLS-1$
			fType= new Text(result, SWT.BORDER);
			gd= new GridData(GridData.FILL_HORIZONTAL);
			fType.setLayoutData(gd);
			fType.setText(fParameter.getNewTypeName());
			fType.addModifyListener(
				new ModifyListener() {
					public void modifyText(ModifyEvent e) {
						validate((Text)e.widget);
					}
				});
		}
		label= new Label(result, SWT.NONE);
		fName= new Text(result, SWT.BORDER);
		initializeDialogUnits(fName);
		if (onlyNameEdit()) {
			label.setText(RefactoringMessages.getFormattedString("ParameterEditDialog.name.message", fParameter.getNewName())); //$NON-NLS-1$
			gd= new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan= 2;
			label.setLayoutData(gd);
			gd= new GridData(GridData.FILL_HORIZONTAL);
			gd.widthHint= convertWidthInCharsToPixels(45);
			gd.horizontalSpan= 2;
			fName.setLayoutData(gd);
		} else {
			label.setText(RefactoringMessages.getString("ParameterEditDialog.all.name")); //$NON-NLS-1$
			gd= new GridData(GridData.FILL_HORIZONTAL);
			gd.widthHint= convertWidthInCharsToPixels(45);
			fName.setLayoutData(gd);
		}
		fName.setText(fParameter.getNewName());
		fName.addModifyListener(
			new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					validate((Text)e.widget);
				}
			});
		if (fParameter.isAdded()) {
			label= new Label(result, SWT.NONE);
			label.setText(RefactoringMessages.getString("ParameterEditDialog.all.defaultValue")); //$NON-NLS-1$
			fDefaultValue= new Text(result, SWT.BORDER);
			gd= new GridData(GridData.FILL_HORIZONTAL);
			fDefaultValue.setLayoutData(gd);
			fDefaultValue.setText(fParameter.getDefaultValue());
			fDefaultValue.addModifyListener(
				new ModifyListener() {
					public void modifyText(ModifyEvent e) {
						validate((Text)e.widget);
					}
				});
		}
		applyDialogFont(result);		
		return result;
	}
	
	protected void okPressed() {
		if (fType != null) {
			fParameter.setNewTypeName(fType.getText());
		}
		fParameter.setNewName(fName.getText());
		if (fDefaultValue != null) {
			fParameter.setDefaultValue(fDefaultValue.getText());
		}
		super.okPressed();
	}
	
	private void validate(Text first) {
		IStatus[] result= new IStatus[3];
		if (first == fType) {
			result[0]= validateType();
			result[1]= validateName();
			result[2]= validateDefaltValue();
		} else if (first == fName) {
			result[0]= validateName();
			result[1]= validateType();
			result[2]= validateDefaltValue();
		} else {
			result[0]= validateDefaltValue();
			result[1]= validateName();
			result[2]= validateType();
		}
		for (int i= 0; i < result.length; i++) {
			IStatus status= result[i];
			if (status != null && !status.isOK()) {
				updateStatus(status);
				return;
			}
		}
		updateStatus(createOkStatus());
	}
	
	private IStatus validateType() {
		if (fType == null)
			return null;
		String typeName= fType.getText();
		if (ChangeSignatureRefactoring.isValidParameterTypeName(typeName))
			return createOkStatus();
		String msg= MessageFormat.format("''{0}'' is not a valid parameter type name", new String[]{typeName});
		return createErrorStatus(msg); 
	}
	
	private Status createErrorStatus(String message) {
		return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.ERROR, message, null);
	}

	private IStatus validateName() {
		if (fName == null) 
			return null;
		String text= fName.getText();
		if (text.length() == 0)
			return createErrorStatus(RefactoringMessages.getString("ParameterEditDialog.all.name.error"));//$NON-NLS-1$
		return JavaConventions.validateFieldName(text);
	}
	
	private IStatus validateDefaltValue() {
		if (fDefaultValue == null)
			return null;
		String s= fDefaultValue.getText();
		if (s.length() == 0) {
			return createErrorStatus(RefactoringMessages.getString("ParameterEditDialog.all.defaultValue.error"));//$NON-NLS-1$
		} else {
			return createOkStatus(); 
		}
	}
	
	private boolean onlyNameEdit() {
		return !fEditType && !fParameter.isAdded();
	}
	
	private Status createOkStatus() {
		return new Status(IStatus.OK, JavaPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$
	}
}
