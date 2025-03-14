// Copyright 2017 The Bazel Authors. All rights reserved.
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

package com.google.devtools.build.lib.packages;

import com.google.devtools.build.lib.syntax.StarlarkSemantics;
import com.google.devtools.common.options.Converters.CommaSeparatedOptionListConverter;
import com.google.devtools.common.options.Option;
import com.google.devtools.common.options.OptionDocumentationCategory;
import com.google.devtools.common.options.OptionEffectTag;
import com.google.devtools.common.options.OptionMetadataTag;
import com.google.devtools.common.options.OptionsBase;
import java.io.Serializable;
import java.util.List;

/**
 * Contains options that affect Starlark's semantics.
 *
 * <p>These are injected into Skyframe (as an instance of {@link StarlarkSemantics}) when a new
 * build invocation occurs. Changing these options between builds will therefore trigger a
 * reevaluation of everything that depends on the Starlark interpreter &mdash; in particular,
 * evaluation of all BUILD and .bzl files.
 *
 * <p><em>To add a new option, update the following:</em>
 *
 * <ul>
 *   <li>Add a new abstract method (which is interpreted by {@code AutoValue} as a field) to {@link
 *       StarlarkSemantics} and {@link StarlarkSemantics.Builder}. Set its default value in {@link
 *       StarlarkSemantics#DEFAULT_SEMANTICS}.
 *   <li>Add a new {@code @Option}-annotated field to this class. The field name and default value
 *       should be the same as in {@link StarlarkSemantics}, and the option name in the annotation
 *       should be that name written in snake_case. Add a line to set the new field in {@link
 *       #toSkylarkSemantics}.
 *   <li>Add a line to set the new field in both {@link
 *       SkylarkSemanticsConsistencyTest#buildRandomOptions} and {@link
 *       SkylarkSemanticsConsistencyTest#buildRandomSemantics}.
 *   <li>Update manual documentation in site/docs/skylark/backward-compatibility.md. Also remember
 *       to update this when flipping a flag's default value.
 *   <li>Boolean semantic flags can toggle Skylark methods on or off. To do this, add a new entry to
 *       {@link StarlarkSemantics#FlagIdentifier}. Then, specify the identifier in {@code
 *       SkylarkCallable.enableOnlyWithFlag} or {@code SkylarkCallable.disableWithFlag}.
 * </ul>
 *
 * For both readability and correctness, the relative order of the options in all of these locations
 * must be kept consistent; to make it easy we use alphabetic order. The parts that need updating
 * are marked with the comment "<== Add new options here in alphabetic order ==>".
 */
public class StarlarkSemanticsOptions extends OptionsBase implements Serializable {

  // <== Add new options here in alphabetic order ==>

  /**
   * This can be overridden by {@link RuleClass.Builder.ThirdPartyLicenseExistencePolicy} and {@link
   * #incompatibleDisableThirdPartyLicenseChecking}.
   */
  @Option(
      name = "check_third_party_targets_have_licenses",
      defaultValue = "true",
      documentationCategory = OptionDocumentationCategory.STARLARK_SEMANTICS,
      effectTags = OptionEffectTag.BUILD_FILE_SEMANTICS,
      help = "If true, all rules and files under //third_party must declare licenses([])")
  public boolean checkThirdPartyTargetsHaveLicenses;

  @Option(
      name = "experimental_build_setting_api",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.UNDOCUMENTED,
      effectTags = OptionEffectTag.BUILD_FILE_SEMANTICS,
      help =
          "If set to true, allows access to value of build setting rules via "
              + "ctx.build_setting_value.")
  public boolean experimentalBuildSettingApi;

  @Option(
      name = "experimental_cc_skylark_api_enabled_packages",
      converter = CommaSeparatedOptionListConverter.class,
      defaultValue = "",
      documentationCategory = OptionDocumentationCategory.UNDOCUMENTED,
      effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
      metadataTags = {OptionMetadataTag.EXPERIMENTAL},
      help =
          "Passes list of packages that can use the C++ Starlark API. Don't enable this flag yet, "
              + "we will be making breaking changes.")
  public List<String> experimentalCcSkylarkApiEnabledPackages;

  @Option(
      name = "experimental_enable_android_migration_apis",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.STARLARK_SEMANTICS,
      effectTags = OptionEffectTag.BUILD_FILE_SEMANTICS,
      help = "If set to true, enables the APIs required to support the Android Starlark migration.")
  public boolean experimentalEnableAndroidMigrationApis;

  // This flag is declared in StarlarkSemanticsOptions instead of JavaOptions because there is no
  // way to retrieve the java configuration from the Java implementation of
  // java_common.create_provider.
  @Option(
      name = "experimental_java_common_create_provider_enabled_packages",
      converter = CommaSeparatedOptionListConverter.class,
      defaultValue = "",
      documentationCategory = OptionDocumentationCategory.UNDOCUMENTED,
      effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
      metadataTags = {OptionMetadataTag.EXPERIMENTAL},
      help = "Passes list of packages that can use the java_common.create_provider Starlark API.")
  public List<String> experimentalJavaCommonCreateProviderEnabledPackages;

  @Option(
      name = "experimental_platforms_api",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.STARLARK_SEMANTICS,
      effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
      metadataTags = {OptionMetadataTag.EXPERIMENTAL},
      help =
          "If set to true, enables a number of platform-related Starlark APIs useful for "
              + "debugging.")
  public boolean experimentalPlatformsApi;

  // TODO(cparsons): Change this flag to --incompatible instead of --experimental when it is
  // fully implemented.
  @Option(
      name = "experimental_restrict_named_params",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.STARLARK_SEMANTICS,
      effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
      metadataTags = {OptionMetadataTag.EXPERIMENTAL},
      help =
          "If set to true, restricts a number of Starlark built-in function parameters to be "
              + "only specifiable positionally (and not by keyword).")
  public boolean experimentalRestrictNamedParams;

  // TODO(cparsons): Resolve and finalize the transition() API. The transition implementation
  // function should accept two mandatory parameters, 'settings' and 'attr'.
  @Option(
      name = "experimental_starlark_config_transitions",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.STARLARK_SEMANTICS,
      effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
      metadataTags = {OptionMetadataTag.EXPERIMENTAL},
      help =
          "If set to true, enables creation of configuration transition objects (the "
              + "`transition()` function) in Starlark.")
  public boolean experimentalStarlarkConfigTransitions;

  @Option(
      name = "incompatible_bzl_disallow_load_after_statement",
      defaultValue = "true",
      documentationCategory = OptionDocumentationCategory.STARLARK_SEMANTICS,
      effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
      metadataTags = {
        OptionMetadataTag.INCOMPATIBLE_CHANGE,
        OptionMetadataTag.TRIGGERED_BY_ALL_INCOMPATIBLE_CHANGES
      },
      help =
          "If set to true, all `load` must be called at the top of .bzl files, before any other "
              + "statement.")
  public boolean incompatibleBzlDisallowLoadAfterStatement;

  @Option(
      name = "incompatible_depset_union",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.STARLARK_SEMANTICS,
      effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
      metadataTags = {
        OptionMetadataTag.INCOMPATIBLE_CHANGE,
        OptionMetadataTag.TRIGGERED_BY_ALL_INCOMPATIBLE_CHANGES
      },
      help =
          "If set to true, depset union using `+`, `|` or `.union` are forbidden. "
              + "Use the `depset` constructor instead.")
  public boolean incompatibleDepsetUnion;

  @Option(
      name = "incompatible_depset_is_not_iterable",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.STARLARK_SEMANTICS,
      effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
      metadataTags = {
        OptionMetadataTag.INCOMPATIBLE_CHANGE,
        OptionMetadataTag.TRIGGERED_BY_ALL_INCOMPATIBLE_CHANGES
      },
      help =
          "If set to true, depset type is not iterable. For loops and functions expecting an "
              + "iterable will reject depset objects. Use the `.to_list` method to explicitly "
              + "convert to a list.")
  public boolean incompatibleDepsetIsNotIterable;

  @Option(
      name = "incompatible_disable_deprecated_attr_params",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.STARLARK_SEMANTICS,
      effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
      metadataTags = {
        OptionMetadataTag.INCOMPATIBLE_CHANGE,
        OptionMetadataTag.TRIGGERED_BY_ALL_INCOMPATIBLE_CHANGES
      },
      help =
          "If set to true, disable the deprecated parameters 'single_file' and 'non_empty' on "
              + "attribute definition methods, such as attr.label().")
  public boolean incompatibleDisableDeprecatedAttrParams;

  @Option(
      name = "incompatible_require_feature_configuration_for_pic",
      defaultValue = "true",
      documentationCategory = OptionDocumentationCategory.TOOLCHAIN,
      effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
      metadataTags = {
        OptionMetadataTag.INCOMPATIBLE_CHANGE,
        OptionMetadataTag.TRIGGERED_BY_ALL_INCOMPATIBLE_CHANGES
      },
      help =
          "If true, cc_toolchain_info.use_pic_for_dynamic_libraries will require "
              + "feature_configuration argument (see #7007).")
  public boolean requireFeatureConfigurationForPic;

  @Option(
      name = "incompatible_disable_objc_provider_resources",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.STARLARK_SEMANTICS,
      effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
      metadataTags = {
        OptionMetadataTag.INCOMPATIBLE_CHANGE,
        OptionMetadataTag.TRIGGERED_BY_ALL_INCOMPATIBLE_CHANGES
      },
      help = "If set to true, disallow use of deprecated resource fields on the Objc provider.")
  public boolean incompatibleDisableObjcProviderResources;

  // Once this migration is complete, instead of removing this flag we need to make it a no-op.
  // This is because we'll need to keep it around for a while so Google's migration can complete
  // after Bazel's. This is an example of Bazel's Google roots being methodically torn out:
  // because this functionality was introduced for Google before Bazel existed, Google's
  // dependency on it is deeper. We don't want this to add unnecessary baggage to Bazel or slow
  // down Bazel's development. So this approach, while slightly awkward, relieves Bazel of
  // Google's technical debt (which shouldn't be Bazel's problem). This means you as a Bazel
  // user are getting better code than Google has! (for a while, at least)
  //
  // Track migration at https://github.com/bazelbuild/bazel/issues/7444. When we're ready to
  // remove Bazel support, instead of removing the flag we should do these things:
  //
  // 1) BazelRuleClassProvider: set the third party license existence policy to NEVER_CHECK (see
  //    the related TODO(gregce) comment in that file).
  // 2) Remove LicenseCheckingModule.
  // 3) Remove --check_third_party_targets_have_licenses.
  @Option(
      name = "incompatible_disable_third_party_license_checking",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.STARLARK_SEMANTICS,
      effectTags = OptionEffectTag.BUILD_FILE_SEMANTICS,
      metadataTags = {
        OptionMetadataTag.INCOMPATIBLE_CHANGE,
        OptionMetadataTag.TRIGGERED_BY_ALL_INCOMPATIBLE_CHANGES
      },
      help =
          "If true, disables all license checking logic. This overrides "
              + "--check_third_party_targets_have_licenses")
  public boolean incompatibleDisableThirdPartyLicenseChecking;

  @Option(
      name = "incompatible_disallow_data_transition",
      defaultValue = "true",
      documentationCategory = OptionDocumentationCategory.STARLARK_SEMANTICS,
      effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
      metadataTags = {
        OptionMetadataTag.INCOMPATIBLE_CHANGE,
        OptionMetadataTag.TRIGGERED_BY_ALL_INCOMPATIBLE_CHANGES
      },
      help = "If set to true, rule attributes cannot set 'cfg = \"data\"', which is a noop.")
  public boolean incompatibleDisallowDataTransition;

  @Option(
      name = "incompatible_disallow_dict_plus",
      defaultValue = "true",
      documentationCategory = OptionDocumentationCategory.STARLARK_SEMANTICS,
      effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
      metadataTags = {
        OptionMetadataTag.INCOMPATIBLE_CHANGE,
        OptionMetadataTag.TRIGGERED_BY_ALL_INCOMPATIBLE_CHANGES
      },
      help = "If set to true, the `+` becomes disabled for dicts.")
  public boolean incompatibleDisallowDictPlus;

  @Option(
      name = "incompatible_disallow_filetype",
      defaultValue = "true",
      documentationCategory = OptionDocumentationCategory.STARLARK_SEMANTICS,
      effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
      metadataTags = {
        OptionMetadataTag.INCOMPATIBLE_CHANGE,
        OptionMetadataTag.TRIGGERED_BY_ALL_INCOMPATIBLE_CHANGES
      },
      help = "If set to true, function `FileType` is not available.")
  public boolean incompatibleDisallowFileType;

  @Option(
      name = "incompatible_disallow_legacy_java_provider",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.STARLARK_SEMANTICS,
      effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
      metadataTags = {
        OptionMetadataTag.INCOMPATIBLE_CHANGE,
        OptionMetadataTag.TRIGGERED_BY_ALL_INCOMPATIBLE_CHANGES
      },
      help = "If set to true, usages of old .java provider are disallowed.")
  public boolean incompatibleDisallowLegacyJavaProvider;

  @Option(
      name = "incompatible_disallow_legacy_javainfo",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.STARLARK_SEMANTICS,
      effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
      metadataTags = {
        OptionMetadataTag.INCOMPATIBLE_CHANGE,
        OptionMetadataTag.TRIGGERED_BY_ALL_INCOMPATIBLE_CHANGES
      },
      help = "If set to true, old-style JavaInfo provider construction is disallowed.")
  public boolean incompatibleDisallowLegacyJavaInfo;

  @Option(
      name = "incompatible_disallow_load_labels_to_cross_package_boundaries",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.STARLARK_SEMANTICS,
      effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
      metadataTags = {
        OptionMetadataTag.INCOMPATIBLE_CHANGE,
        OptionMetadataTag.TRIGGERED_BY_ALL_INCOMPATIBLE_CHANGES
      },
      help = "If set to true, the label argument to 'load' cannot cross a package boundary.")
  public boolean incompatibleDisallowLoadLabelsToCrossPackageBoundaries;

  @Option(
      name = "incompatible_disallow_native_in_build_file",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.STARLARK_SEMANTICS,
      effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
      metadataTags = {
        OptionMetadataTag.INCOMPATIBLE_CHANGE,
        OptionMetadataTag.TRIGGERED_BY_ALL_INCOMPATIBLE_CHANGES
      },
      help =
          "If set to true, the native module is not accessible in BUILD files. "
              + "Use for example `cc_library` instead of `native.cc_library`.")
  public boolean incompatibleDisallowNativeInBuildFile;

  @Option(
      name = "incompatible_disallow_struct_provider_syntax",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.STARLARK_SEMANTICS,
      effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
      metadataTags = {
        OptionMetadataTag.INCOMPATIBLE_CHANGE,
        OptionMetadataTag.TRIGGERED_BY_ALL_INCOMPATIBLE_CHANGES
      },
      help =
          "If set to true, rule implementation functions may not return a struct. They must "
              + "instead return a list of provider instances.")
  public boolean incompatibleDisallowStructProviderSyntax;

  /** Controls legacy arguments to ctx.actions.Args#add. */
  @Option(
      name = "incompatible_disallow_old_style_args_add",
      defaultValue = "true",
      category = "incompatible changes",
      documentationCategory = OptionDocumentationCategory.STARLARK_SEMANTICS,
      effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
      metadataTags = {
        OptionMetadataTag.INCOMPATIBLE_CHANGE,
        OptionMetadataTag.TRIGGERED_BY_ALL_INCOMPATIBLE_CHANGES
      },
      help = "If set to true, vectorized calls to Args#add are disallowed.")
  public boolean incompatibleDisallowOldStyleArgsAdd;

  @Option(
      name = "incompatible_expand_directories",
      defaultValue = "true",
      category = "incompatible changes",
      documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
      effectTags = {OptionEffectTag.UNKNOWN},
      metadataTags = {
        OptionMetadataTag.INCOMPATIBLE_CHANGE,
        OptionMetadataTag.TRIGGERED_BY_ALL_INCOMPATIBLE_CHANGES
      },
      help =
          "Controls whether directories are expanded to the list of files under that directory "
              + "when added to Args, instead of replaced by the path of the directory.")
  public boolean incompatibleExpandDirectories;

  @Option(
      name = "incompatible_new_actions_api",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.STARLARK_SEMANTICS,
      effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
      metadataTags = {
        OptionMetadataTag.INCOMPATIBLE_CHANGE,
        OptionMetadataTag.TRIGGERED_BY_ALL_INCOMPATIBLE_CHANGES
      },
      help =
          "If set to true, the API to create actions is only available on `ctx.actions`, "
              + "not on `ctx`.")
  public boolean incompatibleNewActionsApi;

  @Option(
      name = "incompatible_no_attr_license",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.STARLARK_SEMANTICS,
      effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
      metadataTags = {
        OptionMetadataTag.INCOMPATIBLE_CHANGE,
        OptionMetadataTag.TRIGGERED_BY_ALL_INCOMPATIBLE_CHANGES
      },
      help = "If set to true, disables the function `attr.license`.")
  public boolean incompatibleNoAttrLicense;

  @Option(
      name = "incompatible_no_output_attr_default",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.STARLARK_SEMANTICS,
      effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
      metadataTags = {
        OptionMetadataTag.INCOMPATIBLE_CHANGE,
        OptionMetadataTag.TRIGGERED_BY_ALL_INCOMPATIBLE_CHANGES
      },
      help =
          "If set to true, disables the `default` parameter of the `attr.output` and "
              + "`attr.output_list` attribute definition functions.")
  public boolean incompatibleNoOutputAttrDefault;

  @Option(
      name = "incompatible_no_support_tools_in_action_inputs",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.STARLARK_SEMANTICS,
      effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
      metadataTags = {
        OptionMetadataTag.INCOMPATIBLE_CHANGE,
        OptionMetadataTag.TRIGGERED_BY_ALL_INCOMPATIBLE_CHANGES
      },
      help =
          "If set to true, tools should be passed to `ctx.actions.run()` and "
              + "`ctx.actions.run_shell()` using the `tools` parameter instead of the `inputs` "
              + "parameter. Furthermore, if this flag is set and a `tools` parameter is not "
              + "passed to the action, it is an error for any tools to appear in the `inputs`.")
  public boolean incompatibleNoSupportToolsInActionInputs;

  @Option(
      name = "incompatible_no_target_output_group",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.STARLARK_SEMANTICS,
      effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
      metadataTags = {
        OptionMetadataTag.INCOMPATIBLE_CHANGE,
        OptionMetadataTag.TRIGGERED_BY_ALL_INCOMPATIBLE_CHANGES
      },
      help = "If set to true, disables the output_group field of the 'Target' Starlark type.")
  public boolean incompatibleNoTargetOutputGroup;

  @Option(
      name = "incompatible_no_transitive_loads",
      defaultValue = "true",
      documentationCategory = OptionDocumentationCategory.STARLARK_SEMANTICS,
      effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
      metadataTags = {
        OptionMetadataTag.INCOMPATIBLE_CHANGE,
        OptionMetadataTag.TRIGGERED_BY_ALL_INCOMPATIBLE_CHANGES
      },
      help =
          "If set to true, only symbols explicitly defined in the file can be loaded; "
              + "symbols introduced by load are not implicitly re-exported.")
  public boolean incompatibleNoTransitiveLoads;

  @Option(
      name = "incompatible_remap_main_repo",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.STARLARK_SEMANTICS,
      effectTags = OptionEffectTag.LOADING_AND_ANALYSIS,
      metadataTags = {
        OptionMetadataTag.INCOMPATIBLE_CHANGE,
        OptionMetadataTag.TRIGGERED_BY_ALL_INCOMPATIBLE_CHANGES
      },
      oldName = "experimental_remap_main_repo",
      help = "If set to true, will treat references to '@<main repo name>' the same as '@'.")
  public boolean incompatibleRemapMainRepo;

  @Option(
      name = "incompatible_remove_native_maven_jar",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.STARLARK_SEMANTICS,
      effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
      metadataTags = {
        OptionMetadataTag.INCOMPATIBLE_CHANGE,
        OptionMetadataTag.TRIGGERED_BY_ALL_INCOMPATIBLE_CHANGES
      },
      help =
          "If set to true, the native maven_jar rule is disabled; only the Starlark version "
              + "will be available")
  public boolean incompatibleRemoveNativeMavenJar;

  @Option(
      name = "incompatible_strict_argument_ordering",
      defaultValue = "true",
      documentationCategory = OptionDocumentationCategory.STARLARK_SEMANTICS,
      effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
      metadataTags = {
        OptionMetadataTag.INCOMPATIBLE_CHANGE,
        OptionMetadataTag.TRIGGERED_BY_ALL_INCOMPATIBLE_CHANGES
      },
      help =
          "If set to true, the order of arguments is stricter in function calls, see "
              + "https://github.com/bazelbuild/bazel/issues/6611")
  public boolean incompatibleStricArgumentOrdering;

  /** Used in an integration test to confirm that flags are visible to the interpreter. */
  @Option(
      name = "internal_skylark_flag_test_canary",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.UNDOCUMENTED,
      effectTags = {OptionEffectTag.UNKNOWN})
  public boolean internalSkylarkFlagTestCanary;

  @Option(
      name = "incompatible_use_toolchain_providers_in_java_common",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.STARLARK_SEMANTICS,
      effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
      metadataTags = {
        OptionMetadataTag.INCOMPATIBLE_CHANGE,
        OptionMetadataTag.TRIGGERED_BY_ALL_INCOMPATIBLE_CHANGES
      },
      help =
          "If set to true, java_common APIs that take a java_toolchain or host_javabase parameter "
              + " require a JavaTootoolchainInfo or JavaRuntimeInfo instead of a configured"
              + " target.")
  public boolean incompatibleUseToolchainProvidersInJavaCommon;

  @Option(
      name = "incompatible_do_not_split_linking_cmdline",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.BUILD_TIME_OPTIMIZATION,
      effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
      metadataTags = {
        OptionMetadataTag.INCOMPATIBLE_CHANGE,
        OptionMetadataTag.TRIGGERED_BY_ALL_INCOMPATIBLE_CHANGES
      },
      help =
          "When true, Bazel no longer modifies command line flags used for linking, and also "
              + "doesn't selectively decide which flags go to the param file and which don't.  "
              + "See https://github.com/bazelbuild/bazel/issues/7670 for details.")
  public boolean incompatibleDoNotSplitLinkingCmdline;

  /** Constructs a {@link StarlarkSemantics} object corresponding to this set of option values. */
  public StarlarkSemantics toSkylarkSemantics() {
    return StarlarkSemantics.builder()
        // <== Add new options here in alphabetic order ==>
        .checkThirdPartyTargetsHaveLicenses(checkThirdPartyTargetsHaveLicenses)
        .experimentalBuildSettingApi(experimentalBuildSettingApi)
        .experimentalCcSkylarkApiEnabledPackages(experimentalCcSkylarkApiEnabledPackages)
        .experimentalEnableAndroidMigrationApis(experimentalEnableAndroidMigrationApis)
        .experimentalJavaCommonCreateProviderEnabledPackages(
            experimentalJavaCommonCreateProviderEnabledPackages)
        .experimentalPlatformsApi(experimentalPlatformsApi)
        .experimentalRestrictNamedParams(experimentalRestrictNamedParams)
        .experimentalStarlarkConfigTransitions(experimentalStarlarkConfigTransitions)
        .incompatibleBzlDisallowLoadAfterStatement(incompatibleBzlDisallowLoadAfterStatement)
        .incompatibleDepsetIsNotIterable(incompatibleDepsetIsNotIterable)
        .incompatibleDepsetUnion(incompatibleDepsetUnion)
        .incompatibleDisableThirdPartyLicenseChecking(incompatibleDisableThirdPartyLicenseChecking)
        .incompatibleDisableDeprecatedAttrParams(incompatibleDisableDeprecatedAttrParams)
        .incompatibleDisableObjcProviderResources(incompatibleDisableObjcProviderResources)
        .incompatibleDisallowDataTransition(incompatibleDisallowDataTransition)
        .incompatibleDisallowDictPlus(incompatibleDisallowDictPlus)
        .incompatibleDisallowFileType(incompatibleDisallowFileType)
        .incompatibleDisallowLegacyJavaInfo(incompatibleDisallowLegacyJavaInfo)
        .incompatibleDisallowLegacyJavaProvider(incompatibleDisallowLegacyJavaProvider)
        .incompatibleDisallowLoadLabelsToCrossPackageBoundaries(
            incompatibleDisallowLoadLabelsToCrossPackageBoundaries)
        .incompatibleDisallowNativeInBuildFile(incompatibleDisallowNativeInBuildFile)
        .incompatibleDisallowOldStyleArgsAdd(incompatibleDisallowOldStyleArgsAdd)
        .incompatibleDisallowStructProviderSyntax(incompatibleDisallowStructProviderSyntax)
        .incompatibleExpandDirectories(incompatibleExpandDirectories)
        .incompatibleNewActionsApi(incompatibleNewActionsApi)
        .incompatibleNoAttrLicense(incompatibleNoAttrLicense)
        .incompatibleNoOutputAttrDefault(incompatibleNoOutputAttrDefault)
        .incompatibleNoSupportToolsInActionInputs(incompatibleNoSupportToolsInActionInputs)
        .incompatibleNoTargetOutputGroup(incompatibleNoTargetOutputGroup)
        .incompatibleNoTransitiveLoads(incompatibleNoTransitiveLoads)
        .incompatibleRemapMainRepo(incompatibleRemapMainRepo)
        .incompatibleRemoveNativeMavenJar(incompatibleRemoveNativeMavenJar)
        .incompatibleRequireFeatureConfigurationForPic(requireFeatureConfigurationForPic)
        .incompatibleStricArgumentOrdering(incompatibleStricArgumentOrdering)
        .incompatibleUseToolchainProvidersInJavaCommon(
            incompatibleUseToolchainProvidersInJavaCommon)
        .internalSkylarkFlagTestCanary(internalSkylarkFlagTestCanary)
        .incompatibleDoNotSplitLinkingCmdline(incompatibleDoNotSplitLinkingCmdline)
        .build();
  }
}
