// Copyright 2014 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.devtools.build.lib.rules.objc;

import com.google.common.collect.ImmutableSet;
import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.analysis.ConfiguredTarget;
import com.google.devtools.build.lib.analysis.RuleConfiguredTargetBuilder;
import com.google.devtools.build.lib.analysis.RuleContext;
import com.google.devtools.build.lib.collect.nestedset.NestedSetBuilder;
import com.google.devtools.build.lib.rules.RuleConfiguredTargetFactory;
import com.google.devtools.build.lib.rules.objc.ReleaseBundlingSupport.LinkedBinary;
import com.google.devtools.build.lib.rules.objc.ReleaseBundlingSupport.SplitArchTransition.ConfigurationDistinguisher;

import javax.annotation.Nullable;

/**
 * Base class for rules that bundle releases.
 */
public abstract class ReleaseBundlingTargetFactory implements RuleConfiguredTargetFactory {

  private final String bundleDirFormat;
  private final XcodeProductType xcodeProductType;
  private final ImmutableSet<Attribute> dependencyAttributes;
  private final ConfigurationDistinguisher configurationDistinguisher;

  /**
   * @param bundleDirFormat format string representing the bundle's directory with a single
   *     placeholder for the target name (e.g. {@code "Payload/%s.app"})
   * @param dependencyAttributes all attributes that contain dependencies of this rule. Any
   *     dependency so listed must expose {@link XcodeProvider} and {@link ObjcProvider}.
   * @param configurationDistinguisher distinguisher used for cases where inputs from dependencies
   *     of this bundle may need distinguishing because they come from configurations that are only
   *     different by this value
   */
  public ReleaseBundlingTargetFactory(
      String bundleDirFormat,
      XcodeProductType xcodeProductType,
      ImmutableSet<Attribute> dependencyAttributes,
      ConfigurationDistinguisher configurationDistinguisher) {
    this.bundleDirFormat = bundleDirFormat;
    this.xcodeProductType = xcodeProductType;
    this.dependencyAttributes = dependencyAttributes;
    this.configurationDistinguisher = configurationDistinguisher;
  }

  @Override
  public ConfiguredTarget create(RuleContext ruleContext) throws InterruptedException {
    ObjcCommon common = common(ruleContext);

    XcodeProvider.Builder xcodeProviderBuilder = new XcodeProvider.Builder();
    NestedSetBuilder<Artifact> filesToBuild = NestedSetBuilder.stableOrder();

    ReleaseBundlingSupport releaseBundlingSupport = new ReleaseBundlingSupport(
        ruleContext, common.getObjcProvider(), LinkedBinary.DEPENDENCIES_ONLY, bundleDirFormat,
        bundleName(ruleContext), bundleMinimumOsVersion(ruleContext));
    releaseBundlingSupport
        .registerActions()
        .addXcodeSettings(xcodeProviderBuilder)
        .addFilesToBuild(filesToBuild)
        .validateResources()
        .validateAttributes();

    XcodeSupport xcodeSupport = new XcodeSupport(ruleContext)
        .addFilesToBuild(filesToBuild)
        .addXcodeSettings(xcodeProviderBuilder, common.getObjcProvider(), xcodeProductType,
            ObjcRuleClasses.objcConfiguration(ruleContext).getDependencySingleArchitecture(),
            configurationDistinguisher)
        .addDummySource(xcodeProviderBuilder);

    for (Attribute attribute : dependencyAttributes) {
      xcodeSupport.addDependencies(xcodeProviderBuilder, attribute);
    }

    xcodeSupport.registerActions(xcodeProviderBuilder.build());

    RuleConfiguredTargetBuilder targetBuilder =
        ObjcRuleClasses.ruleConfiguredTarget(ruleContext, filesToBuild.build())
            .addProvider(XcTestAppProvider.class, releaseBundlingSupport.xcTestAppProvider())
            .addProvider(XcodeProvider.class, xcodeProviderBuilder.build());

    ObjcProvider exposedObjcProvider = exposedObjcProvider(ruleContext);
    if (exposedObjcProvider != null) {
      targetBuilder.addProvider(ObjcProvider.class, exposedObjcProvider);
    }

    configureTarget(targetBuilder, ruleContext, releaseBundlingSupport);
    return targetBuilder.build();
  }

  /**
   * Returns the minimum OS version this bundle's plist and resources should be generated for
   * (<b>not</b> the minimum OS version its binary is compiled with, that needs to be set in the
   * configuration).
   */
  protected String bundleMinimumOsVersion(RuleContext ruleContext) {
    return ObjcRuleClasses.objcConfiguration(ruleContext).getMinimumOs();
  }

  /**
   * Performs additional configuration of the target. The default implementation does nothing, but
   * subclasses may override it to add logic.
   */
  protected void configureTarget(RuleConfiguredTargetBuilder target, RuleContext ruleContext,
      ReleaseBundlingSupport releaseBundlingSupport) {}

  /**
   * Returns the name of this target's bundle.
   */
  protected String bundleName(RuleContext ruleContext) {
    return ruleContext.getLabel().getName();
  }

  /**
   * Returns an exposed {@code ObjcProvider} object.
   */
  @Nullable
  protected ObjcProvider exposedObjcProvider(RuleContext ruleContext) {
    return null;
  }

  private ObjcCommon common(RuleContext ruleContext) {
    ObjcCommon.Builder builder = new ObjcCommon.Builder(ruleContext)
        .setIntermediateArtifacts(ObjcRuleClasses.intermediateArtifacts(ruleContext));
    for (Attribute attribute : dependencyAttributes) {
      builder.addDepObjcProviders(
          ruleContext.getPrerequisites(
              attribute.getName(), attribute.getAccessMode(), ObjcProvider.class));
    }
    return builder.build();
  }
}
