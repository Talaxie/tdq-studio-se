// ============================================================================
//
// Copyright (C) 2006-2012 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.dataprofiler.core.ui.action.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.talend.cwm.management.api.FolderProvider;
import org.talend.dataprofiler.core.ImageLib;
import org.talend.dataprofiler.core.i18n.internal.DefaultMessagesImpl;
import org.talend.dataprofiler.core.ui.wizard.analysis.WizardFactory;
import org.talend.dataprofiler.core.ui.wizard.matchrule.NewMatchRuleWizard;
import org.talend.dataquality.indicators.columnset.RecordMatchingIndicator;
import org.talend.dataquality.rules.MatchRuleDefinition;
import org.talend.dq.analysis.parameters.DQMatchRuleParameter;
import org.talend.resource.ResourceManager;

/**
 * DOC yyin class global comment. Detailled comment
 */
public class ExportMatchRuleAction extends Action {

    private MatchRuleDefinition matchRule = null;

    public ExportMatchRuleAction(RecordMatchingIndicator recordMatchingIndicator) {
        ImageDescriptor imageDescriptor = ImageLib.getImageDescriptor(ImageLib.EXPORT_MATCH_RULE_ICON);
        setText(DefaultMessagesImpl.getString("MatchAnalysisEditor.exportMatchRule")); //$NON-NLS-1$
        this.setImageDescriptor(imageDescriptor);

        this.matchRule = recordMatchingIndicator.getBuiltInMatchRuleDefinition();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.action.Action#run()
     */
    @Override
    public void run() {
        // if there are no match rule, or no keys in the match rule.
        if ((matchRule == null)
                || ((matchRule.getBlockKeys() == null || matchRule.getBlockKeys().size() < 1) && (matchRule.getMatchRules() == null || matchRule
                        .getMatchRules().size() < 1))) {

            MessageDialog.openWarning(null, DefaultMessagesImpl.getString("ExportMatchRuleAction.noRule"), //$NON-NLS-1$
                    DefaultMessagesImpl.getString("ExportMatchRuleAction.noKeys")); //$NON-NLS-1$
            return;
        }

        DQMatchRuleParameter parameter = new DQMatchRuleParameter();
        FolderProvider folderProvider = new FolderProvider();
        folderProvider.setFolderResource(ResourceManager.getRulesMatcherFolder());
        parameter.setFolderProvider(folderProvider);
        NewMatchRuleWizard matchWizard = WizardFactory.createNewMatchRuleWizard(parameter);
        matchWizard.setWindowTitle(getText());

        matchWizard.setMatchRule(matchRule);

        WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), matchWizard);
        dialog.open();
    }

}
