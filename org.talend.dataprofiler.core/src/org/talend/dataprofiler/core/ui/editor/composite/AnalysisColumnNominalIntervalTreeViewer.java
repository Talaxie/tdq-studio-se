// ============================================================================
//
// Copyright (C) 2006-2007 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.dataprofiler.core.ui.editor.composite;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TreeAdapter;
import org.eclipse.swt.events.TreeEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.FileEditorInput;
import org.talend.cwm.helper.TaggedValueHelper;
import org.talend.cwm.relational.TdColumn;
import org.talend.dataprofiler.core.ImageLib;
import org.talend.dataprofiler.core.PluginConstant;
import org.talend.dataprofiler.core.i18n.internal.DefaultMessagesImpl;
import org.talend.dataprofiler.core.manager.DQStructureManager;
import org.talend.dataprofiler.core.model.ColumnIndicator;
import org.talend.dataprofiler.core.ui.editor.AbstractAnalysisActionHandler;
import org.talend.dataprofiler.core.ui.editor.AbstractMetadataFormPage;
import org.talend.dataprofiler.core.ui.editor.analysis.ColumnCorrelationNominalAndIntervalMasterPage;
import org.talend.dataprofiler.core.ui.editor.preview.IndicatorUnit;
import org.talend.dataprofiler.core.ui.utils.FormEnum;
import org.talend.dataprofiler.core.ui.utils.OpeningHelpWizardDialog;
import org.talend.dataprofiler.core.ui.views.ColumnViewerDND;
import org.talend.dataprofiler.core.ui.wizard.indicator.IndicatorOptionsWizard;
import org.talend.dataquality.analysis.Analysis;
import org.talend.dataquality.domain.Domain;
import org.talend.dataquality.domain.pattern.Pattern;
import org.talend.dataquality.helpers.MetadataHelper;
import org.talend.dataquality.indicators.DataminingType;
import org.talend.dataquality.indicators.DateParameters;
import org.talend.dataquality.indicators.IndicatorParameters;
import org.talend.dataquality.indicators.PatternMatchingIndicator;
import org.talend.dataquality.indicators.TextParameters;
import org.talend.dq.helper.resourcehelper.PatternResourceFileHelper;
import org.talend.dq.nodes.indicator.type.IndicatorEnum;
import orgomg.cwm.resource.relational.Column;

/**
 * 
 * DOC zhaoxinyi class global comment. Detailled comment
 */
public class AnalysisColumnNominalIntervalTreeViewer extends AbstractColumnDropTree {

    private static final String DATA_PARAM = "DATA_PARAM"; //$NON-NLS-1$

    public static final String INDICATOR_UNIT_KEY = "INDICATOR_UNIT_KEY"; //$NON-NLS-1$

    public static final String COLUMN_INDICATOR_KEY = "COLUMN_INDICATOR_KEY"; //$NON-NLS-1$

    public static final String ITEM_EDITOR_KEY = "ITEM_EDITOR_KEY"; //$NON-NLS-1$

    public static final String VIEWER_KEY = "org.talend.dataprofiler.core.ui.editor.composite.AnalysisColumnNominalIntervalTreeViewer"; //$NON-NLS-1$

    private static final int WIDTH1_CELL = 75;

    private Composite parentComp;

    private boolean isLast;

    private Tree tree;

    private List<Column> columnSetMultiValueList;

    // private final List<String> comboTextList = new ArrayList<String>();

    private ColumnCorrelationNominalAndIntervalMasterPage masterPage;

    private Menu menu;

    private MenuItem editPatternMenuItem;

    public AnalysisColumnNominalIntervalTreeViewer(Composite parent) {
        parentComp = parent;
        this.tree = createTree(parent);
        initTreeData(tree);
    }

    public AnalysisColumnNominalIntervalTreeViewer(Composite parent, ColumnCorrelationNominalAndIntervalMasterPage masterPage) {
        this(parent);
        this.masterPage = masterPage;
        // this.setElements(masterPage.getColumnSetMultiValueIndicator().getAnalyzedColumns());
        this.setDirty(false);
    }

    /**
     * @param parent
     */
    private Tree createTree(Composite parent) {
        final Tree newTree = new Tree(parent, SWT.MULTI | SWT.BORDER);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(newTree);

        newTree.setHeaderVisible(true);
        TreeColumn column1 = new TreeColumn(newTree, SWT.CENTER);
        column1.setWidth(190);
        column1.setText(DefaultMessagesImpl.getString("AnalysisColumnTreeViewer.analyzedColumns")); //$NON-NLS-1$
        TreeColumn column2 = new TreeColumn(newTree, SWT.CENTER);
        column2.setWidth(120);
        column2.setText(DefaultMessagesImpl.getString("AnalysisColumnTreeViewer.dataminingType")); //$NON-NLS-1$
        column2.setToolTipText(DefaultMessagesImpl.getString("AnalysisColumnTreeViewer.columnTip")); //$NON-NLS-1$
        /*
         * TreeColumn column3 = new TreeColumn(newTree, SWT.CENTER); column3.setWidth(80);
         * column3.setText(DefaultMessagesImpl.getString("AnalysisColumnTreeViewer.pattern")); //$NON-NLS-1$
         */TreeColumn column4 = new TreeColumn(newTree, SWT.CENTER);
        column4.setWidth(80);
        column4.setText(DefaultMessagesImpl.getString("AnalysisColumnTreeViewer.operation")); //$NON-NLS-1$

        parent.layout();
        createTreeMenu(newTree, false);

        AbstractAnalysisActionHandler actionHandler = new AbstractAnalysisActionHandler(parent) {

            @Override
            protected void handleRemove() {
                removeSelectedElements(newTree);
            }

        };

        parent.setData(AbstractMetadataFormPage.ACTION_HANDLER, actionHandler);
        ColumnViewerDND.installDND(newTree);
        this.addTreeListener(newTree);
        return newTree;
    }

    /**
     * DOC qzhang Comment method "createTreeMenu".
     * 
     * @param newTree
     * @param containEdit
     */
    private void createTreeMenu(final Tree newTree, boolean containEdit) {
        Menu oldMenu = newTree.getMenu();
        if (oldMenu != null && !oldMenu.isDisposed()) {
            oldMenu.dispose();
        }
        menu = new Menu(newTree);
        MenuItem deleteMenuItem = new MenuItem(menu, SWT.CASCADE);
        deleteMenuItem.setText(DefaultMessagesImpl.getString("AnalysisColumnTreeViewer.removeElement")); //$NON-NLS-1$
        deleteMenuItem.setImage(ImageLib.getImage(ImageLib.DELETE_ACTION));
        deleteMenuItem.addSelectionListener(new SelectionAdapter() {

            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            @Override
            public void widgetSelected(SelectionEvent e) {
                removeSelectedElements(newTree);
            }

        });
        if (containEdit) {
            editPatternMenuItem = new MenuItem(menu, SWT.CASCADE);
            editPatternMenuItem.setText(DefaultMessagesImpl.getString("AnalysisColumnTreeViewer.editPattern")); //$NON-NLS-1$
            editPatternMenuItem.setImage(ImageLib.getImage(ImageLib.PATTERN_REG));
            editPatternMenuItem.addSelectionListener(new SelectionAdapter() {

                /*
                 * (non-Javadoc)
                 * 
                 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
                 */
                @Override
                public void widgetSelected(SelectionEvent e) {
                    TreeItem[] selection = tree.getSelection();
                    if (selection.length > 0) {
                        TreeItem treeItem = selection[0];
                        IndicatorUnit indicatorUnit = (IndicatorUnit) treeItem.getData(INDICATOR_UNIT_KEY);
                        PatternMatchingIndicator indicator = (PatternMatchingIndicator) indicatorUnit.getIndicator();
                        Pattern pattern = indicator.getParameters().getDataValidDomain().getPatterns().get(0);
                        IFolder patternFolder = ResourcesPlugin.getWorkspace().getRoot().getProject(DQStructureManager.LIBRARIES)
                                .getFolder(DQStructureManager.PATTERNS);
                        IFolder sqlPatternFolder = ResourcesPlugin.getWorkspace().getRoot().getProject(
                                DQStructureManager.LIBRARIES).getFolder(DQStructureManager.SQL_PATTERNS);
                        IFile file = PatternResourceFileHelper.getInstance().getPatternFile(pattern,
                                new IFolder[] { patternFolder, sqlPatternFolder });
                        IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                        try {
                            activePage.openEditor(new FileEditorInput(file),
                                    "org.talend.dataprofiler.core.ui.editor.pattern.PatternEditor"); //$NON-NLS-1$
                        } catch (PartInitException e1) {
                            e1.printStackTrace();
                        }
                    }
                }

            });
        }
        newTree.setMenu(menu);
    }

    public void setInput(Object[] objs) {
        if (objs != null && objs.length != 0) {
            if (!(objs[0] instanceof TdColumn)) {
                return;
            }
        }
        List<Column> columnList = new ArrayList<Column>();
        for (Object obj : objs) {
            columnList.add((TdColumn) obj);
        }
        this.setElements(columnList);
    }

    public void setElements(final List<Column> columns) {
        this.tree.dispose();
        this.tree = createTree(this.parentComp);
        tree.setData(VIEWER_KEY, this);
        this.columnSetMultiValueList = columns;
        addItemElements(columns);
    }

    private void addItemElements(final List<Column> columns) {
        for (int i = 0; i < columns.size(); i++) {
            final TdColumn column = (TdColumn) columns.get(i);
            final TreeItem treeItem = new TreeItem(tree, SWT.NONE);
            String columnName = column.getName();
            treeItem.setImage(ImageLib.getImage(ImageLib.TD_COLUMN));

            treeItem.setText(0, columnName != null ? columnName + PluginConstant.SPACE_STRING + PluginConstant.PARENTHESIS_LEFT
                    + column.getSqlDataType().getName() + PluginConstant.PARENTHESIS_RIGHT : "null"); //$NON-NLS-1$
            treeItem.setData(COLUMN_INDICATOR_KEY, column);

            TreeEditor comboEditor = new TreeEditor(tree);
            final CCombo combo = new CCombo(tree, SWT.BORDER);
            for (DataminingType type : DataminingType.values()) {
                combo.add(type.getLiteral()); // MODSCA 2008-04-10 use literal for presentation
            }
            DataminingType dataminingType = MetadataHelper.getDataminingType(column);
            if (dataminingType == null) {
                dataminingType = MetadataHelper.getDefaultDataminingType(column.getJavaType());
            }

            if (dataminingType == null) {
                combo.select(0);
            } else {
                combo.setText(dataminingType.getLiteral());
            }

            /**
             * 
             * DOC zhaoxinyi AnalysisColumnNominalIntervalTreeViewer class global comment. Detailled comment
             */
            class Selection extends SelectionAdapter {

                public void widgetSelected(SelectionEvent e) {
                    MetadataHelper.setDataminingType(DataminingType.get(combo.getText()), column);
                    setDirty(true);
                }
            }
            combo.addSelectionListener(new Selection());
            // comboTextList.add(combo.getText().trim());
            combo.setEditable(false);
            comboEditor.minimumWidth = WIDTH1_CELL;
            comboEditor.setEditor(combo, treeItem, 1);

            TreeEditor delLabelEditor = new TreeEditor(tree);
            Label delLabel = new Label(tree, SWT.NONE);
            delLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
            delLabel.setImage(ImageLib.getImage(ImageLib.DELETE_ACTION));
            delLabel.setToolTipText(DefaultMessagesImpl.getString("AnalysisColumnTreeViewer.delete")); //$NON-NLS-1$
            delLabel.pack();
            delLabel.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseDown(MouseEvent e) {
                    deleteColumnItems(column);
                    setElements(columns);
                }

            });

            delLabelEditor.minimumWidth = WIDTH1_CELL;
            delLabelEditor.horizontalAlignment = SWT.CENTER;
            delLabelEditor.setEditor(delLabel, treeItem, 2);
            treeItem.setData(ITEM_EDITOR_KEY, new TreeEditor[] { comboEditor, delLabelEditor });
            /*
             * if (columnIndicator.hasIndicators()) { createIndicatorItems(treeItem,
             * columnIndicator.getIndicatorUnits()); }
             */
            treeItem.setExpanded(true);
        }
        this.setDirty(true);
    }

    public void addElements(final List<Column> columns) {
        this.addItemElements(columns);
    }

    private void createIndicatorItems(final TreeItem treeItem, IndicatorUnit[] indicatorUnits) {
        for (IndicatorUnit indicatorUnit : indicatorUnits) {
            createOneUnit(treeItem, indicatorUnit);
        }
    }

    /**
     * DOC qzhang Comment method "createOneUnit".
     * 
     * @param treeItem
     * @param indicatorUnit
     */
    public void createOneUnit(final TreeItem treeItem, IndicatorUnit indicatorUnit) {
        final TreeItem indicatorItem = new TreeItem(treeItem, SWT.NONE);
        final IndicatorUnit unit = indicatorUnit;
        IndicatorEnum type = indicatorUnit.getType();
        final IndicatorEnum indicatorEnum = type;
        indicatorItem.setData(COLUMN_INDICATOR_KEY, treeItem.getData(COLUMN_INDICATOR_KEY));
        indicatorItem.setData(INDICATOR_UNIT_KEY, unit);
        indicatorItem.setData(VIEWER_KEY, this);
        String label = indicatorUnit.getIndicatorName();
        if (IndicatorEnum.RegexpMatchingIndicatorEnum.compareTo(type) == 0
                || IndicatorEnum.SqlPatternMatchingIndicatorEnum.compareTo(type) == 0) {
            indicatorItem.setImage(0, ImageLib.getImage(ImageLib.PATTERN_REG));
        }
        indicatorItem.setText(0, label);

        TreeEditor optionEditor;
        // if (indicatorEnum.hasChildren()) {
        optionEditor = new TreeEditor(tree);
        Label optionLabel = new Label(tree, SWT.NONE);
        optionLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        optionLabel.setImage(ImageLib.getImage(ImageLib.INDICATOR_OPTION));
        optionLabel.setToolTipText(DefaultMessagesImpl.getString("AnalysisColumnTreeViewer.options")); //$NON-NLS-1$
        optionLabel.pack();
        optionLabel.setData(indicatorUnit);
        optionLabel.addMouseListener(new MouseAdapter() {

            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.swt.events.MouseAdapter#mouseDown(org.eclipse.swt.events.MouseEvent)
             */
            @Override
            public void mouseDown(MouseEvent e) {

                IndicatorUnit indicatorUnit = (IndicatorUnit) ((Label) e.getSource()).getData();
                IndicatorOptionsWizard wizard = new IndicatorOptionsWizard(indicatorUnit);

                if (FormEnum.isExsitingForm(indicatorUnit)) {
                    String href = FormEnum.getFirstFormHelpHref(indicatorUnit);
                    OpeningHelpWizardDialog optionDialog = new OpeningHelpWizardDialog(null, wizard, href);
                    optionDialog.create();
                    if (Window.OK == optionDialog.open()) {
                        setDirty(wizard.isDirty());
                        createIndicatorParameters(indicatorItem, indicatorUnit);
                    }
                } else {
                    MessageDialogWithToggle
                            .openInformation(
                                    null,
                                    DefaultMessagesImpl.getString("AnalysisColumnTreeViewer.information"), DefaultMessagesImpl.getString("AnalysisColumnTreeViewer.nooption")); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }

        });

        optionEditor.minimumWidth = WIDTH1_CELL;
        optionEditor.horizontalAlignment = SWT.CENTER;
        optionEditor.setEditor(optionLabel, indicatorItem, 1);
        // }

        TreeEditor delEditor = new TreeEditor(tree);
        Label delLabel = new Label(tree, SWT.NONE);
        delLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        delLabel.setImage(ImageLib.getImage(ImageLib.DELETE_ACTION));
        delLabel.setToolTipText(DefaultMessagesImpl.getString("AnalysisColumnTreeViewer.delete")); //$NON-NLS-1$
        delLabel.pack();
        delLabel.addMouseListener(new MouseAdapter() {

            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.swt.events.MouseAdapter#mouseDown(org.eclipse.swt.events.MouseEvent)
             */
            @Override
            public void mouseDown(MouseEvent e) {
                ColumnIndicator columnIndicator = (ColumnIndicator) treeItem.getData(COLUMN_INDICATOR_KEY);
                deleteIndicatorItems(columnIndicator, unit);
                if (indicatorItem.getParentItem() != null && indicatorItem.getParentItem().getData(INDICATOR_UNIT_KEY) != null) {
                    setElements(columnSetMultiValueList);
                } else {
                    removeItemBranch(indicatorItem);
                }
            }

        });

        delEditor.minimumWidth = WIDTH1_CELL;
        delEditor.horizontalAlignment = SWT.CENTER;
        delEditor.setEditor(delLabel, indicatorItem, 3);
        indicatorItem.setData(ITEM_EDITOR_KEY, new TreeEditor[] { optionEditor, delEditor });
        if (indicatorEnum.hasChildren()) {
            indicatorItem.setData(treeItem.getData(COLUMN_INDICATOR_KEY));
            createIndicatorItems(indicatorItem, indicatorUnit.getChildren());
        }
        createIndicatorParameters(indicatorItem, indicatorUnit);
    }

    /**
     * DOC qzhang Comment method "createIndicatorParameters".
     * 
     * @param indicatorItem
     * @param parameters
     */
    private void createIndicatorParameters(TreeItem indicatorItem, IndicatorUnit indicatorUnit) {
        TreeItem[] items = indicatorItem.getItems();
        if (indicatorItem != null && !indicatorItem.isDisposed()) {
            for (TreeItem treeItem : items) {
                if (DATA_PARAM.equals(treeItem.getData(DATA_PARAM))) {
                    treeItem.dispose();
                }
            }
        }
        IndicatorParameters parameters = indicatorUnit.getIndicator().getParameters();
        if (parameters == null) {
            return;
        }
        TreeItem iParamItem;
        if (indicatorUnit.getType() == IndicatorEnum.FrequencyIndicatorEnum) {
            iParamItem = new TreeItem(indicatorItem, SWT.NONE);
            iParamItem.setText(0, DefaultMessagesImpl.getString("AnalysisColumnTreeViewer.resultsShown") + parameters.getTopN()); //$NON-NLS-1$
            iParamItem.setData(DATA_PARAM, DATA_PARAM);
            iParamItem.setImage(0, ImageLib.getImage(ImageLib.OPTION));
        }

        TextParameters tParameter = parameters.getTextParameter();
        if (tParameter != null) {
            iParamItem = new TreeItem(indicatorItem, SWT.NONE);
            iParamItem.setText(0, DefaultMessagesImpl.getString("AnalysisColumnTreeViewer.textParameters")); //$NON-NLS-1$
            iParamItem.setData(DATA_PARAM, DATA_PARAM);
            iParamItem.setImage(0, ImageLib.getImage(ImageLib.OPTION));

            TreeItem subParamItem = new TreeItem(iParamItem, SWT.NONE);
            subParamItem.setText(DefaultMessagesImpl.getString("AnalysisColumnTreeViewer.useBlanks") + tParameter.isUseBlank()); //$NON-NLS-1$
            subParamItem.setImage(0, ImageLib.getImage(ImageLib.OPTION));
            subParamItem.setData(DATA_PARAM, DATA_PARAM);

            subParamItem = new TreeItem(iParamItem, SWT.NONE);
            subParamItem
                    .setText(DefaultMessagesImpl.getString("AnalysisColumnTreeViewer.ignoreCase") + tParameter.isIgnoreCase()); //$NON-NLS-1$
            subParamItem.setImage(0, ImageLib.getImage(ImageLib.OPTION));
            subParamItem.setData(DATA_PARAM, DATA_PARAM);

            subParamItem = new TreeItem(iParamItem, SWT.NONE);
            subParamItem.setText(DefaultMessagesImpl.getString("AnalysisColumnTreeViewer.useNulls") + tParameter.isUseNulls()); //$NON-NLS-1$
            subParamItem.setImage(0, ImageLib.getImage(ImageLib.OPTION));
            subParamItem.setData(DATA_PARAM, DATA_PARAM);
        }
        DateParameters dParameters = parameters.getDateParameters();
        if (dParameters != null) {
            iParamItem = new TreeItem(indicatorItem, SWT.NONE);
            iParamItem.setText(0, DefaultMessagesImpl.getString("AnalysisColumnTreeViewer.dateParameters")); //$NON-NLS-1$
            iParamItem.setData(DATA_PARAM, DATA_PARAM);
            iParamItem.setImage(0, ImageLib.getImage(ImageLib.OPTION));

            TreeItem subParamItem = new TreeItem(iParamItem, SWT.NONE);
            subParamItem.setText(DefaultMessagesImpl.getString(
                    "AnalysisColumnTreeViewer.aggregationType", dParameters.getDateAggregationType().getName())); //$NON-NLS-1$ //$NON-NLS-2$
            subParamItem.setImage(0, ImageLib.getImage(ImageLib.OPTION));
            subParamItem.setData(DATA_PARAM, DATA_PARAM);
        }

        Domain dataValidDomain = parameters.getDataValidDomain();
        if (dataValidDomain != null) {
            iParamItem = new TreeItem(indicatorItem, SWT.NONE);
            iParamItem.setText(0,
                    DefaultMessagesImpl.getString("AnalysisColumnTreeViewer.validDomain") + (dataValidDomain != null)); //$NON-NLS-1$
            iParamItem.setData(DATA_PARAM, DATA_PARAM);
            iParamItem.setImage(0, ImageLib.getImage(ImageLib.OPTION));
        }
        Domain indicatorValidDomain = parameters.getIndicatorValidDomain();
        if (indicatorValidDomain != null) {
            iParamItem = new TreeItem(indicatorItem, SWT.NONE);
            iParamItem.setText(0,
                    DefaultMessagesImpl.getString("AnalysisColumnTreeViewer.qualityThresholds") + (indicatorValidDomain != null)); //$NON-NLS-1$
            iParamItem.setData(DATA_PARAM, DATA_PARAM);
            iParamItem.setImage(0, ImageLib.getImage(ImageLib.OPTION));
        }
        Domain bins = parameters.getBins();
        if (bins != null) {
            iParamItem = new TreeItem(indicatorItem, SWT.NONE);
            iParamItem.setText(0, DefaultMessagesImpl.getString("AnalysisColumnTreeViewer.binsDefined") + (bins != null)); //$NON-NLS-1$
            iParamItem.setData(DATA_PARAM, DATA_PARAM);
            iParamItem.setImage(0, ImageLib.getImage(ImageLib.OPTION));
        }
    }

    /**
     * DOC rli Comment method "deleteIndicatorItems".
     * 
     * @param treeItem
     * @param inidicatorUnit
     */
    private void deleteIndicatorItems(ColumnIndicator columnIndicator, IndicatorUnit inidicatorUnit) {
        columnIndicator.removeIndicatorUnit(inidicatorUnit);
    }

    /**
     * DOC rli Comment method "deleteTreeElements".
     * 
     * @param columnIndicators
     * @param deleteColumnIndiciators
     */
    private void deleteColumnItems(TdColumn deleteColumn) {
        List<Column> remainColumns = columnSetMultiValueList;
        for (int j = 0; j < columnSetMultiValueList.size(); j++) {
            TdColumn column = (TdColumn) columnSetMultiValueList.get(j);
            if (deleteColumn.equals(column)) {
                remainColumns.remove(j);
            }
        }
        setElements(remainColumns);
        if (remainColumns.size() == 0)
            isLast = true;
        this.columnSetMultiValueList = remainColumns;
    }

    /*
     * public void openIndicatorSelectDialog(Shell shell) { final IndicatorSelectDialog dialog = new
     * IndicatorSelectDialog(shell, DefaultMessagesImpl .getString("AnalysisColumnTreeViewer.indicatorSelection"),
     * columnIndicators); //$NON-NLS-1$ dialog.create(); dialog.getShell().addShellListener(new ShellAdapter() {
     * 
     * 
     * (non-Javadoc)
     * 
     * @see org.eclipse.swt.events.ShellAdapter#shellActivated(org.eclipse.swt.events.ShellEvent)
     * 
     * @Override public void shellActivated(ShellEvent e) { dialog.getShell().setFocus(); IContext context =
     * HelpSystem.getContext(HelpPlugin.getDefault().getIndicatorSelectorHelpContextID());
     * PlatformUI.getWorkbench().getHelpSystem().displayHelp(context); } });
     * 
     * if (dialog.open() == Window.OK) { ColumnIndicator[] result = dialog.getResult(); for (ColumnIndicator
     * columnIndicator : result) { columnIndicator.storeTempIndicator(); } this.setElements(result); return; } }
     */

    public List<Column> getColumnSetMultiValueList() {
        return this.columnSetMultiValueList;
    }

    /**
     * Remove the selected elements(eg:TdColumn or Indicator) from tree.
     * 
     * @param newTree
     */
    private void removeSelectedElements(final Tree newTree) {
        TreeItem[] selection = newTree.getSelection();
        for (TreeItem item : selection) {
            TdColumn tdColumn = (TdColumn) item.getData(COLUMN_INDICATOR_KEY);
            deleteColumnItems(tdColumn);

        }
    }

    private void removeItemBranch(TreeItem item) {
        TreeEditor[] editors = (TreeEditor[]) item.getData(ITEM_EDITOR_KEY);
        if (editors != null) {
            for (int j = 0; j < editors.length; j++) {
                editors[j].getEditor().dispose();
                editors[j].dispose();
            }
        }

        if (item.getItemCount() == 0) {
            item.dispose();
            this.setDirty(true);
            return;
        }
        TreeItem[] items = item.getItems();
        for (int i = 0; i < items.length; i++) {
            removeItemBranch(items[i]);
        }
        item.dispose();
        this.setDirty(true);
    }

    private void addTreeListener(final Tree tree) {
        tree.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean con = false;

                if (e.item instanceof TreeItem) {
                    TreeItem item = (TreeItem) e.item;
                    if (DATA_PARAM.equals(item.getData(DATA_PARAM))) {
                        tree.setMenu(null);
                        return;
                    } else if (item.getData(INDICATOR_UNIT_KEY) != null) {
                        IndicatorUnit indicatorUnit = (IndicatorUnit) item.getData(INDICATOR_UNIT_KEY);
                        IndicatorEnum type = indicatorUnit.getType();
                        con = IndicatorEnum.RegexpMatchingIndicatorEnum.compareTo(type) == 0
                                || IndicatorEnum.SqlPatternMatchingIndicatorEnum.compareTo(type) == 0;
                    }
                }
                createTreeMenu(tree, con);
            }

        });

        tree.addTreeListener(new TreeAdapter() {

            @Override
            public void treeCollapsed(TreeEvent e) {

                ExpandableComposite theSuitedComposite = getTheSuitedComposite(e);
                ScrolledForm form = masterPage.getForm();
                Composite comp = masterPage.getChartComposite();

                if (theSuitedComposite != null && theSuitedComposite.isExpanded()) {
                    getTheSuitedComposite(e).setExpanded(false);
                }

                comp.layout();
                form.reflow(true);
            }

            @Override
            public void treeExpanded(TreeEvent e) {
                ExpandableComposite theSuitedComposite = getTheSuitedComposite(e);
                ScrolledForm form = masterPage.getForm();
                Composite comp = masterPage.getChartComposite();

                if (theSuitedComposite != null && !theSuitedComposite.isExpanded()) {
                    theSuitedComposite.setExpanded(true);
                }

                comp.layout();
                form.reflow(true);
            }

        });
    }

    private ExpandableComposite getTheSuitedComposite(SelectionEvent e) {
        Composite[] previewChartCompsites = masterPage.getPreviewChartCompsites();
        if (previewChartCompsites == null) {
            return null;
        }

        Object obj = e.item.getData(COLUMN_INDICATOR_KEY);
        if (obj instanceof ColumnIndicator) {
            ColumnIndicator columnIndicator = (ColumnIndicator) obj;
            for (Composite comp : previewChartCompsites) {
                if (comp.getData() == columnIndicator) {
                    return (ExpandableComposite) comp;
                }
            }
        }

        return null;
    }

    /**
     * DOC zqin AnalysisColumnTreeViewer class global comment. Detailled comment
     */
    class PatternLabelProvider extends LabelProvider {

        @Override
        public Image getImage(Object element) {
            if (element instanceof IFolder) {
                return ImageLib.getImage(ImageLib.FOLDERNODE_IMAGE);
            }

            if (element instanceof IFile) {
                Pattern findPattern = PatternResourceFileHelper.getInstance().findPattern((IFile) element);
                boolean validStatus = TaggedValueHelper.getValidStatus(findPattern);
                ImageDescriptor imageDescriptor = ImageLib.getImageDescriptor(ImageLib.PATTERN_REG);
                if (!validStatus) {
                    ImageDescriptor warnImg = PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
                            ISharedImages.IMG_OBJS_WARN_TSK);
                    DecorationOverlayIcon icon = new DecorationOverlayIcon(imageDescriptor.createImage(), warnImg,
                            IDecoration.BOTTOM_RIGHT);
                    imageDescriptor = icon;
                }
                return imageDescriptor.createImage();
            }

            return null;
        }

        @Override
        public String getText(Object element) {
            if (element instanceof IFile) {
                IFile file = (IFile) element;
                Pattern pattern = PatternResourceFileHelper.getInstance().findPattern(file);
                if (pattern != null) {
                    return pattern.getName();
                }
            }

            if (element instanceof IFolder) {
                return ((IFolder) element).getName();
            }

            return ""; //$NON-NLS-1$
        }
    }

    /**
     * Getter for analysis.
     * 
     * @return the analysis
     */
    public Analysis getAnalysis() {
        return this.masterPage.getColumnCorrelationAnalysisHandler().getAnalysis();
    }

    // public List<String> getComboString() {
    // return comboTextList;
    // }

    public Tree getTree() {
        return tree;
    }

    @Override
    public boolean canDrop(Column column) {
        List<TdColumn> existColumns = new ArrayList<TdColumn>();
        for (Column columnFromMultiValueList : this.getColumnSetMultiValueList()) {
            existColumns.add((TdColumn) columnFromMultiValueList);
        }

        if (existColumns.contains(column)) {
            return false;
        }
        return true;
    }

    @Override
    public void dropColumns(List<Column> columns) {
        this.addElements(columns);
    }
}
