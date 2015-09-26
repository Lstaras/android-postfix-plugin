package com.kogitune.intellij.codeinsight.postfix.macro;

import com.android.ide.common.res2.ResourceItem;
import com.android.resources.ResourceType;
import com.android.tools.idea.rendering.LocalResourceRepository;
import com.android.tools.idea.rendering.ProjectResourceRepository;
import com.intellij.codeInsight.template.*;
import com.intellij.codeInsight.template.impl.ConstantNode;
import com.intellij.codeInsight.template.impl.MacroCallNode;
import com.intellij.codeInsight.template.macro.VariableOfTypeMacro;
import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import org.apache.http.util.TextUtils;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

import static com.kogitune.intellij.codeinsight.postfix.utils.AndroidClassName.*;

/**
 * Created by takam on 2015/05/05.
 */
public class FindViewByIdMacro extends Macro {


    public String getName() {
        return "find_view";
    }

    public String getPresentableName() {
        return "find_view";
    }

    @Nullable
    @Override
    public Result calculateResult(Expression[] expressions, ExpressionContext context) {
        if (expressions.length == 0) {
            return null;
        }

        Project project = context.getProject();
        Expression expression = expressions[0];
        final String resource = expression.calculateResult(context).toString();
        final TextResult defaultResult = new TextResult("findViewById(" + resource + ")");
        if (!resource.startsWith("R.id.")) {
            return defaultResult;
        }

        final int index = resource.lastIndexOf(".");
        final String resourceId = resource.substring(index + 1);

        String viewTag = getViewTag(project, resourceId);
        if (viewTag == null) {
            return defaultResult;
        }
        final String contextVariable = getContextVariable(context);
        if (contextVariable == null) {
            return new TextResult("(" + viewTag + ")findViewById(" + resource + ")");
        } else {
            return new TextResult("(" + viewTag + ")" + contextVariable + ".findViewById(" + resource + ")");
        }


    }

    private String getContextVariable(ExpressionContext context) {
        Result calculateResult = getVariableByFQDN(context, ACTIVITY.toString());
        if (calculateResult == null) {
            // Retry by view
            calculateResult = getVariableByFQDN(context, VIEW.toString());
            if (calculateResult == null) {
                return null;
            }
        }
        final String result = calculateResult.toString();
        if (TextUtils.isEmpty(result)) {
            return null;
        }
        if ("this".equals(result)) {
            return null;
        }
        return result;
    }

    private Result getVariableByFQDN(ExpressionContext context, String fqn) {
        MacroCallNode callNode = new MacroCallNode(new VariableOfTypeMacro());
        callNode.addParameter(new ConstantNode(fqn));
        return callNode.calculateResult(context);
    }


    @Nullable
    public String getViewTag(Project project, String resourceId) {
        final ModuleManager moduleManager = ModuleManager.getInstance(project);
        List<Module> modules = Arrays.asList(moduleManager.getModules());
        final Module module = modules.get(1);


        AndroidFacet androidFacet = null;
        for (Facet facet : FacetManager.getInstance(module).getAllFacets()) {
            if (AndroidFacet.class.getName().equals(facet.getClass().getName())) {
                androidFacet = (AndroidFacet) facet;
            }
        }
        final LocalResourceRepository resources = ProjectResourceRepository.getProjectResources(androidFacet, true);


        List<ResourceItem> items = resources.getResourceItem(ResourceType.ID, resourceId);

        if (items.size() == 0) {
            return null;
        }
        final ResourceItem resourceItem = items.get(0);
        final String viewTag = resources.getViewTag(resourceItem);
        return viewTag;
    }
}
