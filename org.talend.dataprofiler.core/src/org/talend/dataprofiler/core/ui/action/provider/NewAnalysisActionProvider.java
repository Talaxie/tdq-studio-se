// ============================================================================
//
// Copyright (C) 2006-2010 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.dataprofiler.core.ui.action.provider;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFolder;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.TreeSelection;
import org.talend.dataprofiler.core.ui.action.actions.CreateNewAnalysisAction;
import org.talend.dataprofiler.core.ui.utils.WorkbenchUtils;
import org.talend.repository.model.IRepositoryNode.ENodeType;
import org.talend.repository.model.RepositoryNode;
import org.talend.resource.ResourceManager;
import org.talend.resource.ResourceService;

/**
 * @author rli
 * 
 */
public class NewAnalysisActionProvider extends AbstractCommonActionProvider {

    protected static Logger log = Logger.getLogger(NewAnalysisActionProvider.class);

    public NewAnalysisActionProvider() {
    }

    /**
     * Adds a submenu to the given menu with the name "New Component".
     */
    public void fillContextMenu(IMenuManager menu) {
        // MOD mzhao user readonly role on svn repository mode.
        if (!isShowMenu()) {
            return;
        }
        // Object obj = ((TreeSelection) this.getContext().getSelection()).getFirstElement();
        // if (obj instanceof IFolder) {
        // IFolder folder = (IFolder) obj;
        //
        // if (ResourceService.isSubFolder(ResourceManager.getAnalysisFolder(), folder)) {
        // CreateNewAnalysisAction createAnalysisAction = new CreateNewAnalysisAction(folder);
        // menu.add(createAnalysisAction);
        // }
        // }

        // DOC klliu 2010-11-23 set the node and path to the action
        Object obj = ((TreeSelection) this.getContext().getSelection()).getFirstElement();
        RepositoryNode node = (RepositoryNode) obj;
        if (ENodeType.SYSTEM_FOLDER.equals(node.getType()) || ENodeType.SIMPLE_FOLDER.equals(node.getType())) {
            IFolder ifolder = WorkbenchUtils.getFolder(node);
            if (ifolder != null && (ResourceService.isSubFolder(ResourceManager.getAnalysisFolder(), ifolder))) {
                CreateNewAnalysisAction createAnalysisAction = new CreateNewAnalysisAction(WorkbenchUtils.getPath(node), node);
                menu.add(createAnalysisAction);
            }
        }
    }
}
