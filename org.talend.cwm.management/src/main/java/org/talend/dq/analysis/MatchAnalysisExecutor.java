// ============================================================================
//
// Copyright (C) 2006-2013 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.dq.analysis;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.EList;
import org.talend.core.model.metadata.builder.connection.MetadataColumn;
import org.talend.cwm.db.connection.DatabaseSQLExecutor;
import org.talend.cwm.db.connection.DelimitedFileSQLExecutor;
import org.talend.cwm.db.connection.ISQLExecutor;
import org.talend.cwm.db.connection.MDMSQLExecutor;
import org.talend.cwm.management.i18n.Messages;
import org.talend.cwm.relational.TdColumn;
import org.talend.cwm.xml.TdXmlElementType;
import org.talend.dataquality.PluginConstant;
import org.talend.dataquality.analysis.Analysis;
import org.talend.dataquality.indicators.Indicator;
import org.talend.dataquality.indicators.columnset.BlockKeyIndicator;
import org.talend.dataquality.indicators.columnset.RecordMatchingIndicator;
import org.talend.dataquality.record.linkage.genkey.BlockingKeyHandler;
import org.talend.dataquality.record.linkage.grouping.AnalysisMatchRecordGrouping;
import org.talend.dataquality.record.linkage.grouping.MatchGroupResultConsumer;
import org.talend.dataquality.record.linkage.utils.AnalysisRecordGroupingUtils;
import org.talend.dataquality.record.linkage.utils.MatchAnalysisConstant;
import org.talend.dataquality.rules.AppliedBlockKey;
import org.talend.dataquality.rules.BlockKeyDefinition;
import org.talend.dataquality.rules.KeyDefinition;
import org.talend.dataquality.rules.MatchKeyDefinition;
import org.talend.dataquality.rules.MatchRule;
import org.talend.dataquality.rules.RulesPackage;
import org.talend.utils.sugars.ReturnCode;
import orgomg.cwm.objectmodel.core.ModelElement;

/**
 * created by zhao on Aug 20, 2013 Detailled comment
 * 
 */
public class MatchAnalysisExecutor implements IAnalysisExecutor {

    private static Logger log = Logger.getLogger(MatchAnalysisExecutor.class);

    private IProgressMonitor monitor;

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dq.analysis.IAnalysisExecutor#execute(org.talend.dataquality.analysis.Analysis)
     */
    public ReturnCode execute(Analysis analysis) {
        ReturnCode rc = new ReturnCode(Boolean.TRUE);
        EList<Indicator> indicators = analysis.getResults().getIndicators();
        RecordMatchingIndicator recordMatchingIndicator = null;
        BlockKeyIndicator blockKeyIndicator = null;
        for (Indicator ind : indicators) {
            if (ind instanceof RecordMatchingIndicator) {
                recordMatchingIndicator = (RecordMatchingIndicator) ind;
            } else if (ind instanceof BlockKeyIndicator) {
                blockKeyIndicator = (BlockKeyIndicator) ind;
            }
        }
        if (recordMatchingIndicator == null || blockKeyIndicator == null) {
            rc.setOk(Boolean.FALSE);
            return rc;
        }

        List<ModelElement> anlayzedElements = analysis.getContext().getAnalysedElements();
        if (anlayzedElements == null || anlayzedElements.size() == 0) {
            rc.setOk(Boolean.FALSE);
            // TODO yyin popup message to notify empty analyzed element
            return rc;
        }
        Map<String, String> columnMap = getColumn2IndexMap(anlayzedElements);

        MatchGroupResultConsumer matchResultConsumer = createMatchGroupResultConsumer(columnMap, recordMatchingIndicator);

        ISQLExecutor sqlExecutor = getSQLExectutor(anlayzedElements);
        if (sqlExecutor == null) {
            rc.setOk(Boolean.FALSE);
            return rc;
        }
        List<Object[]> matchRows = new ArrayList<Object[]>();
        try {
            monitor.beginTask(Messages.getString("MatchAnalysisExecutor.FETCH_DATA"), 0); //$NON-NLS-1$
            matchRows = sqlExecutor.executeQuery(analysis.getContext().getConnection(), analysis.getContext()
                    .getAnalysedElements());
        } catch (SQLException e) {
            log.error(e, e);
            rc.setOk(Boolean.FALSE);
            return rc;
        }

        monitor.worked(20);

        List<AppliedBlockKey> appliedBlockKeys = recordMatchingIndicator.getBuiltInMatchRuleDefinition().getAppliedBlockKeys();
        appliedBlockKeys.clear();
        // By default for analysis, the applied blocking key will be the key from key generation definition. This
        // will be refined when there is a need to define the applied blocking key manually by user later.
        createAppliedBlockKeyByGenKey(recordMatchingIndicator, appliedBlockKeys);

        // Blocking key specified.
        computeMatchGroupWithBlockKey(analysis, recordMatchingIndicator, blockKeyIndicator, columnMap, matchResultConsumer,
                matchRows);
        monitor.worked(100);
        monitor.done();
        return rc;
    }

    /**
     * DOC zhao Comment method "createAppliedBlockKeyByGenKey".
     * 
     * @param recordMatchingIndicator
     * @param appliedBlockKeys
     */
    private void createAppliedBlockKeyByGenKey(RecordMatchingIndicator recordMatchingIndicator,
            List<AppliedBlockKey> appliedBlockKeys) {
        List<BlockKeyDefinition> blockKeyDefs = recordMatchingIndicator.getBuiltInMatchRuleDefinition().getBlockKeys();
        if (blockKeyDefs != null && blockKeyDefs.size() > 0) {
            AppliedBlockKey appliedBlockKey = RulesPackage.eINSTANCE.getRulesFactory().createAppliedBlockKey();
            appliedBlockKey.setColumn(PluginConstant.BLOCK_KEY);
            appliedBlockKey.setName(PluginConstant.BLOCK_KEY);
            appliedBlockKeys.add(appliedBlockKey);
        }
    }

    /**
     * DOC zhao Comment method "computeMatchGroupWithBlockKey".
     * 
     * @param analysis
     * @param recordMatchingIndicator
     * @param blockKeyIndicator
     * @param columnMap
     * @param matchResultConsumer
     * @param matchRows
     */
    private void computeMatchGroupWithBlockKey(Analysis analysis, RecordMatchingIndicator recordMatchingIndicator,
            BlockKeyIndicator blockKeyIndicator, Map<String, String> columnMap, MatchGroupResultConsumer matchResultConsumer,
            List<Object[]> matchRows) {
        Map<String, List<String[]>> resultWithBlockKey = computeBlockingKey(analysis, columnMap, matchRows,
                recordMatchingIndicator);
        Iterator<String> keyIterator = resultWithBlockKey.keySet().iterator();

        TreeMap<Object, Long> blockSize2Freq = new TreeMap<Object, Long>();
        while (keyIterator.hasNext()) {
            // Match group with in each block
            List<String[]> matchRowsInBlock = resultWithBlockKey.get(keyIterator.next());
            List<Object[]> objList = new ArrayList<Object[]>();
            objList.addAll(matchRowsInBlock);
            computeMatchGroupResult(columnMap, matchResultConsumer, objList, recordMatchingIndicator);

            // Store indicator
            Integer blockSize = matchRowsInBlock.size();
            if (blockSize == null) { // should not happen
                blockSize = 0;
            }
            Long freq = blockSize2Freq.get(Long.valueOf(blockSize));
            if (freq == null) {
                freq = 0l;
            }
            blockSize2Freq.put(Long.valueOf(blockSize), freq + 1);
        }
        blockKeyIndicator.setBlockSize2frequency(blockSize2Freq);
    }

    /**
     * DOC zhao Comment method "getColumn2IndexMap".
     * 
     * @param anlayzedElements
     * @return
     */
    private Map<String, String> getColumn2IndexMap(List<ModelElement> anlayzedElements) {
        Map<String, String> columnMap = new HashMap<String, String>();
        int index = 0;
        for (ModelElement column : anlayzedElements) {
            columnMap.put(column.getName(), String.valueOf(index++));
        }
        return columnMap;
    }

    private Map<String, List<String[]>> computeBlockingKey(Analysis analysis, Map<String, String> columnMap,
            List<Object[]> matchRows, RecordMatchingIndicator recordMatchingIndicator) {
        List<AppliedBlockKey> appliedBlockKeys = recordMatchingIndicator.getBuiltInMatchRuleDefinition().getAppliedBlockKeys();

        List<Map<String, String>> blockKeySchema = new ArrayList<Map<String, String>>();
        for (KeyDefinition keyDef : appliedBlockKeys) {
            AppliedBlockKey appliedKeyDefinition = (AppliedBlockKey) keyDef;
            String column = appliedKeyDefinition.getColumn();

            Map<String, String> blockKeyDefMap = new HashMap<String, String>();
            blockKeyDefMap.put(MatchAnalysisConstant.COLUMN, column);
            if (StringUtils.equals(PluginConstant.BLOCK_KEY, column)) {
                // If there exist customized block key defined, get the key parameters.
                List<BlockKeyDefinition> blockKeyDefs = recordMatchingIndicator.getBuiltInMatchRuleDefinition().getBlockKeys();
                for (BlockKeyDefinition blockKeyDef : blockKeyDefs) {
                    blockKeyDefMap.putAll(getCustomizedBlockKeyParameter(blockKeyDef, blockKeyDef.getColumn()));
                }
            }
            blockKeySchema.add(blockKeyDefMap);
        }

        BlockingKeyHandler blockKeyHandler = new BlockingKeyHandler(blockKeySchema, columnMap);
        blockKeyHandler.setInputData(matchRows);
        blockKeyHandler.run();
        Map<String, List<String[]>> resultData = blockKeyHandler.getResultDatas();

        return resultData;

    }

    /**
     * DOC zhao Comment method "getCustomizedBlockKeyParameter".
     * 
     * @param appliedKeyDefinition
     * @param column
     * @return
     */
    private Map<String, String> getCustomizedBlockKeyParameter(BlockKeyDefinition blockKeydef, String column) {
        String preAlgo = blockKeydef.getPreAlgorithm().getAlgorithmType();
        String preAlgoValue = blockKeydef.getPreAlgorithm().getAlgorithmParameters();
        String algorithm = blockKeydef.getAlgorithm().getAlgorithmType();
        String algorithmValue = blockKeydef.getAlgorithm().getAlgorithmParameters();
        String postAlgo = blockKeydef.getPostAlgorithm().getAlgorithmType();
        String postAlgValue = blockKeydef.getPostAlgorithm().getAlgorithmParameters();
        Map<String, String> blockKeyDefMap = AnalysisRecordGroupingUtils.getBlockingKeyMap(column, preAlgo, preAlgoValue,
                algorithm, algorithmValue, postAlgo, postAlgValue);
        return blockKeyDefMap;
    }

    /**
     * DOC zhao Comment method "computeMatchGroupResult".
     * 
     * @param columnMap
     * @param matchResultConsumer
     * @param matchRows
     */
    private void computeMatchGroupResult(Map<String, String> columnMap, MatchGroupResultConsumer matchResultConsumer,
            List<Object[]> matchRows, RecordMatchingIndicator recordMatchingIndicator) {
        AnalysisMatchRecordGrouping analysisMatchRecordGrouping = new AnalysisMatchRecordGrouping(matchResultConsumer);
        List<MatchRule> matchRules = recordMatchingIndicator.getBuiltInMatchRuleDefinition().getMatchRules();
        for (MatchRule matcher : matchRules) {
            List<Map<String, String>> ruleMatcherConvertResult = new ArrayList<Map<String, String>>();
            if (matcher == null) {
                continue;
            }
            for (MatchKeyDefinition matchDef : matcher.getMatchKeys()) {
                Map<String, String> matchKeyMap = AnalysisRecordGroupingUtils.getMatchKeyMap(matchDef.getColumn(), matchDef
                        .getAlgorithm().getAlgorithmType(), matchDef.getConfidenceWeight(), columnMap,
                        matcher.getMatchInterval(), matchDef.getColumn());
                ruleMatcherConvertResult.add(matchKeyMap);
            }
            analysisMatchRecordGrouping.addRuleMatcher(ruleMatcherConvertResult);
        }
        analysisMatchRecordGrouping.setMatchRows(matchRows);
        analysisMatchRecordGrouping.run();

    }

    /**
     * DOC zhao Comment method "initRecordMatchIndicator".
     * 
     * @param columnMap
     * @return
     */
    private MatchGroupResultConsumer createMatchGroupResultConsumer(Map<String, String> columnMap,
            final RecordMatchingIndicator recordMatchingIndicator) {
        recordMatchingIndicator.setMatchRowSchema(AnalysisRecordGroupingUtils.getCompleteColumnSchema(columnMap));
        recordMatchingIndicator.reset();
        MatchGroupResultConsumer matchResultConsumer = new MatchGroupResultConsumer() {

            /*
             * (non-Javadoc)
             * 
             * @see org.talend.dataquality.record.linkage.grouping.MatchGroupResultConsumer#handle(java.lang.Object)
             */
            @Override
            public void handle(Object row) {
                recordMatchingIndicator.handle(row);

            }
        };
        return matchResultConsumer;
    }

    /**
     * DOC zhao Comment method "getSQLExectutor".
     * 
     * @param modelElement
     * @return
     */
    private ISQLExecutor getSQLExectutor(List<ModelElement> anlayzedElements) {
        ModelElement modelElement = anlayzedElements.get(0);
        ISQLExecutor sqlExecutor = null;
        if (modelElement instanceof TdColumn) {
            sqlExecutor = new DatabaseSQLExecutor();
        } else if (modelElement instanceof TdXmlElementType) {
            sqlExecutor = new MDMSQLExecutor();
        } else if (modelElement instanceof MetadataColumn) {
            sqlExecutor = new DelimitedFileSQLExecutor();
        }
        return sqlExecutor;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dq.analysis.IAnalysisExecutor#setMonitor(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void setMonitor(IProgressMonitor monitor) {
        this.monitor = monitor;
    }
}
