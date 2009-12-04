// ============================================================================
//
// Copyright (C) 2006-2009 Talend Inc. - www.talend.com
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

import java.sql.Connection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.talend.cwm.helper.CatalogHelper;
import org.talend.cwm.helper.ColumnSetHelper;
import org.talend.cwm.helper.ResourceHelper;
import org.talend.cwm.helper.SchemaHelper;
import org.talend.cwm.helper.SwitchHelpers;
import org.talend.cwm.management.i18n.Messages;
import org.talend.cwm.relational.TdCatalog;
import org.talend.cwm.relational.TdSchema;
import org.talend.cwm.softwaredeployment.TdDataProvider;
import org.talend.dataquality.analysis.Analysis;
import org.talend.dataquality.indicators.Indicator;
import org.talend.dq.indicators.IndicatorEvaluator;
import org.talend.dq.sql.converters.CwmZQuery;
import org.talend.utils.sugars.ReturnCode;
import org.talend.utils.sugars.TypedReturnCode;
import orgomg.cwm.objectmodel.core.ModelElement;
import orgomg.cwm.objectmodel.core.Package;
import orgomg.cwm.resource.relational.ColumnSet;
import orgomg.cwm.resource.relational.NamedColumnSet;

/**
 * DOC xqliu class global comment. Detailled comment
 */
public class TableAnalysisExecutor extends AnalysisExecutor {

    private TdDataProvider dataprovider;

    private static Logger log = Logger.getLogger(TableAnalysisExecutor.class);

    protected Map<ModelElement, Package> schemata = new HashMap<ModelElement, Package>();

    protected boolean isAccessWith(TdDataProvider dp) {
        if (dataprovider == null) {
            dataprovider = dp;
            return true;
        }
        if (ResourceHelper.areSame(dataprovider, dp)) {
            return true;
        }
        return false;
    }

    @Override
    protected String createSqlStatement(Analysis analysis) {
        this.cachedAnalysis = analysis;
        CwmZQuery query = new CwmZQuery();
        EList<ModelElement> analysedElements = analysis.getContext().getAnalysedElements();
        if (analysedElements.isEmpty()) {
            this.errorMessage = Messages.getString("ColumnAnalysisExecutor.CannotCreateSQLStatement",//$NON-NLS-1$
                    analysis.getName());
            return null;
        }
        Set<ColumnSet> fromPart = new HashSet<ColumnSet>();
        // Set<ColumnSet> wherePart = new HashSet<ColumnSet>();
        for (ModelElement modelElement : analysedElements) {
            NamedColumnSet set = SwitchHelpers.NAMED_COLUMN_SET_SWITCH.doSwitch(modelElement);
            if (set == null) {
                this.errorMessage = Messages.getString("TableAnalysisExecutor.NoContainerFound", modelElement.getName()); //$NON-NLS-1$
                return null;
            }
            // else add into select
            if (!query.addSelect(set)) {
                this.errorMessage = Messages.getString("TableAnalysisExecutor.Problem"); //$NON-NLS-1$
                return null;
            }
            // add from
            fromPart.add(set);
            // TODO add where part
        }

        if (!query.addFrom(fromPart)) {
            this.errorMessage = Messages.getString("TableAnalysisExecutor.ProblemAddFromPart"); //$NON-NLS-1$
            return null;
        }
        return query.generateStatement();
    }

    @Override
    protected boolean runAnalysis(Analysis analysis, String sqlStatement) {
        IndicatorEvaluator eval = new IndicatorEvaluator();
        eval.setMonitor(getMonitor());
        // --- add indicators
        EList<Indicator> indicators = analysis.getResults().getIndicators();
        for (Indicator indicator : indicators) {
            assert indicator != null;
            NamedColumnSet set = SwitchHelpers.NAMED_COLUMN_SET_SWITCH.doSwitch(indicator.getAnalyzedElement());
            if (set == null) {
                continue;
            }
            // --- get the schema owner
            if (!belongToSameSchemata(set)) {
                this.errorMessage = Messages.getString("TableAnalysisExecutor.GivenTable", set.getName()); //$NON-NLS-1$
                return false;
            }
            String setName = set.getName();

            // --- normalize table name
            String schemaName = getQuotedSchemaName(set);
            String catalogName = getQuotedCatalogName(set);
            if (catalogName == null && schemaName != null) {
                // try to get catalog above schema
                final TdSchema parentSchema = SchemaHelper.getParentSchema(set);
                final TdCatalog parentCatalog = CatalogHelper.getParentCatalog(parentSchema);
                catalogName = parentCatalog != null ? parentCatalog.getName() : null;
            }

            setName = dbms().toQualifiedName(catalogName, schemaName, setName);

            eval.storeIndicator(setName, indicator);
        }

        // open a connection
        TypedReturnCode<Connection> connection = getConnection(analysis);
        if (!connection.isOk()) {
            log.error(connection.getMessage());
            this.errorMessage = connection.getMessage();
            return false;
        }

        // set it into the evaluator
        eval.setConnection(connection.getObject());
        // when to close connection
        boolean closeAtTheEnd = true;
        Package catalog = schemata.values().iterator().next();
        if (!eval.selectCatalog(catalog.getName())) {
            log.warn("Failed to select catalog " + catalog.getName() + " for connection.");
        }
        ReturnCode rc = eval.evaluateIndicators(sqlStatement, closeAtTheEnd);
        if (!rc.isOk()) {
            log.warn(rc.getMessage());
            this.errorMessage = rc.getMessage();
        }
        return rc.isOk();
    }

    protected boolean belongToSameSchemata(final NamedColumnSet set) {
        assert set != null;
        if (schemata.get(set) != null) {
            return true;
        }
        // get catalog or schema
        Package schema = ColumnSetHelper.getParentCatalogOrSchema(set);
        if (schema == null) {
            this.errorMessage = Messages.getString("TableAnalysisExecutor.NoSchemaOrCatalogFound", set.getName()); //$NON-NLS-1$
            return false;
        }
        schemata.put(set, schema);
        return true;
    }
}
