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

package com.google.devtools.build.lib.rules.cpp;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.analysis.ConfiguredTarget;
import com.google.devtools.build.lib.analysis.RuleContext;
import com.google.devtools.build.lib.analysis.config.CompilationMode;
import com.google.devtools.build.lib.analysis.platform.ToolchainInfo;
import com.google.devtools.build.lib.analysis.util.AnalysisMock;
import com.google.devtools.build.lib.analysis.util.BuildViewTestCase;
import com.google.devtools.build.lib.packages.util.Crosstool.CcToolchainConfig;
import com.google.devtools.build.lib.packages.util.MockCcSupport;
import com.google.devtools.build.lib.packages.util.ResourceLoader;
import com.google.devtools.build.lib.util.Pair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit tests for {@code CcToolchainProvider}
 */
@RunWith(JUnit4.class)
public class CcToolchainProviderTest extends BuildViewTestCase {
  @Test
  public void testSkylarkCallables() throws Exception {
    AnalysisMock.get()
        .ccSupport()
        .setupCcToolchainConfig(
            mockToolsConfig, CcToolchainConfig.builder().withFeatures(CppRuleClasses.SUPPORTS_PIC));
    useConfiguration("--cpu=k8", "--force_pic");
    scratch.file(
        "test/rule.bzl",
        "def _impl(ctx):",
        "  provider = ctx.attr._cc_toolchain[cc_common.CcToolchainInfo]",
        "  feature_configuration = cc_common.configure_features(cc_toolchain = provider)",
        "  return struct(",
        "    dirs = provider.built_in_include_directories,",
        "    sysroot = provider.sysroot,",
        "    cpu = provider.cpu,",
        "    ar_executable = provider.ar_executable,",
        "    use_pic_for_dynamic_libraries = provider.needs_pic_for_dynamic_libraries(",
        "      feature_configuration = feature_configuration,",
        "    ),",
        "  )",
        "",
        "my_rule = rule(",
        "  _impl,",
        "  attrs = {'_cc_toolchain': attr.label(default=Label('//test:toolchain')) }",
        ")");

    scratch.file("test/BUILD",
        "load(':rule.bzl', 'my_rule')",
        "cc_toolchain_alias(name = 'toolchain')",
        "my_rule(name = 'target')");

    ConfiguredTarget ct = getConfiguredTarget("//test:target");

    assertThat((String) ct.get("ar_executable")).endsWith("/usr/bin/mock-ar");

    assertThat(ct.get("cpu")).isEqualTo("k8");

    assertThat(ct.get("sysroot")).isEqualTo("/usr/grte/v1");

    @SuppressWarnings("unchecked")
    boolean usePicForDynamicLibraries = (boolean) ct.get("use_pic_for_dynamic_libraries");
    assertThat(usePicForDynamicLibraries).isTrue();
  }

  @Test
  public void testRemoveCpuAndCompiler() throws Exception {
    scratch.file(
        "a/BUILD",
        "load(':cc_toolchain_config.bzl', 'cc_toolchain_config')",
        "filegroup(name = 'empty')",
        "cc_toolchain_suite(",
        "    name = 'a_suite',",
        "    toolchains = { 'k8': ':a' },",
        ")",
        "cc_toolchain_suite(",
        "    name = 'b_suite',",
        "    toolchains = { 'k9': ':b', },",
        ")",
        "cc_toolchain_suite(",
        "    name = 'c_suite',",
        "    toolchains = { 'k10': ':c', },",
        ")",
        "cc_toolchain(",
        "    name = 'a',",
        "    cpu = 'banana',",
        "    all_files = ':empty',",
        "    ar_files = ':empty',",
        "    as_files = ':empty',",
        "    compiler_files = ':empty',",
        "    dwp_files = ':empty',",
        "    linker_files = ':empty',",
        "    strip_files = ':empty',",
        "    objcopy_files = ':empty',",
        "    toolchain_identifier = 'banana',",
        "    toolchain_config = ':banana_config',",
        ")",
        "cc_toolchain(",
        "    name = 'b',",
        "    compiler = 'banana',",
        "    all_files = ':empty',",
        "    ar_files = ':empty',",
        "    as_files = ':empty',",
        "    compiler_files = ':empty',",
        "    dwp_files = ':empty',",
        "    linker_files = ':empty',",
        "    strip_files = ':empty',",
        "    objcopy_files = ':empty',",
        "    toolchain_identifier = 'banana',",
        "    toolchain_config = ':banana_config',",
        ")",
        "cc_toolchain(",
        "    name = 'c',",
        "    all_files = ':empty',",
        "    ar_files = ':empty',",
        "    as_files = ':empty',",
        "    compiler_files = ':empty',",
        "    dwp_files = ':empty',",
        "    linker_files = ':empty',",
        "    strip_files = ':empty',",
        "    objcopy_files = ':empty',",
        "    toolchain_identifier = 'banana',",
        "    toolchain_config = ':banana_config',",
        ")",
        "cc_toolchain_config(name = 'banana_config')");

    scratch.file("a/cc_toolchain_config.bzl", MockCcSupport.EMPTY_CC_TOOLCHAIN);

    reporter.removeHandler(failFastHandler);
    useConfiguration(
        "--crosstool_top=//a:a_suite",
        "--cpu=k8",
        "--host_cpu=k8",
        "--incompatible_remove_cpu_and_compiler_attributes_from_cc_toolchain");
    assertThat(getConfiguredTarget("//a:a_suite")).isNull();
    assertContainsEvent(
        "attributes 'cpu' and 'compiler' have been deprecated, please remove them.");
    eventCollector.clear();

    useConfiguration(
        "--crosstool_top=//a:b_suite",
        "--cpu=k9",
        "--host_cpu=k9",
        "--incompatible_remove_cpu_and_compiler_attributes_from_cc_toolchain");
    assertThat(getConfiguredTarget("//a:b_suite")).isNull();
    assertContainsEvent(
        "attributes 'cpu' and 'compiler' have been deprecated, please remove them.");
    eventCollector.clear();

    useConfiguration(
        "--crosstool_top=//a:c_suite",
        "--cpu=k10",
        "--host_cpu=k10",
        "--incompatible_remove_cpu_and_compiler_attributes_from_cc_toolchain");
    getConfiguredTarget("//a:c_suite");
    assertNoEvents();
  }

  @Test
  public void testDisablingCompilationModeFlags() throws Exception {
    reporter.removeHandler(failFastHandler);
    AnalysisMock.get()
        .ccSupport()
        .setupCrosstool(
            mockToolsConfig,
            "compilation_mode_flags { mode: OPT compiler_flag: '-foo_from_compilation_mode' }",
            "compilation_mode_flags { mode: OPT cxx_flag: '-bar_from_compilation_mode' }",
            "compilation_mode_flags { mode: OPT linker_flag: '-baz_from_compilation_mode' }");
    scratch.file("a/BUILD", "cc_library(name='a', srcs=['a.cc'])");

    useConfiguration(
        "-c",
        "opt",
        "--noincompatible_disable_legacy_crosstool_fields",
        "--noincompatible_disable_crosstool_file");
    CcToolchainProvider ccToolchainProvider = getCcToolchainProvider();
    assertThat(ccToolchainProvider.getLegacyCompileOptionsWithCopts())
        .contains("-foo_from_compilation_mode");
    assertThat(ccToolchainProvider.getLegacyCxxOptions()).contains("-bar_from_compilation_mode");
    assertThat(ccToolchainProvider.getLegacyMostlyStaticLinkFlags(CompilationMode.OPT))
        .contains("-baz_from_compilation_mode");

    useConfiguration(
        "-c",
        "opt",
        "--incompatible_disable_legacy_crosstool_fields",
        "--noincompatible_disable_crosstool_file");
    getConfiguredTarget("//a");
    assertContainsEvent(
        "compilation_mode_flags is disabled by "
            + "--incompatible_disable_legacy_crosstool_fields, please migrate your CROSSTOOL");
  }

  private CcToolchainProvider getCcToolchainProvider() throws Exception {
    ConfiguredTarget target = getConfiguredTarget("//a");
    RuleContext ruleContext = getRuleContext(target);
    return CppHelper.getToolchainUsingDefaultCcToolchainAttribute(ruleContext);
  }

  @Test
  public void testDisablingLinkingModeFlags() throws Exception {
    reporter.removeHandler(failFastHandler);
    AnalysisMock.get()
        .ccSupport()
        .setupCrosstool(
            mockToolsConfig,
            "linking_mode_flags { mode: MOSTLY_STATIC linker_flag: '-foo_from_linking_mode' }");
    scratch.file("a/BUILD", "cc_library(name='a', srcs=['a.cc'])");

    useConfiguration(
        "--noincompatible_disable_legacy_crosstool_fields",
        "--noincompatible_disable_crosstool_file");
    CcToolchainProvider ccToolchainProvider = getCcToolchainProvider();
    assertThat(ccToolchainProvider.getLegacyMostlyStaticLinkFlags(CompilationMode.OPT))
        .contains("-foo_from_linking_mode");

    useConfiguration(
        "--incompatible_disable_legacy_crosstool_fields",
        "--noincompatible_disable_crosstool_file");
    getConfiguredTarget("//a");
    assertContainsEvent(
        "linking_mode_flags is disabled by "
            + "--incompatible_disable_legacy_crosstool_fields, please migrate your CROSSTOOL");
  }

  @Test
  public void testDisablingLegacyCrosstoolFields() throws Exception {
    reporter.removeHandler(failFastHandler);
    AnalysisMock.get()
        .ccSupport()
        .setupCrosstool(mockToolsConfig, "compiler_flag: '-foo_compiler_flag'");
    scratch.file("a/BUILD", "cc_library(name='a', srcs=['a.cc'])");

    useConfiguration(
        "--noincompatible_disable_legacy_crosstool_fields",
        "--noincompatible_disable_crosstool_file");
    CcToolchainProvider ccToolchainProvider = getCcToolchainProvider();
    assertThat(ccToolchainProvider.getLegacyCompileOptions()).contains("-foo_compiler_flag");

    useConfiguration(
        "--incompatible_disable_legacy_crosstool_fields",
        "--noincompatible_disable_crosstool_file");
    getConfiguredTarget("//a");

    assertContainsEvent(
        "compiler_flag is disabled by --incompatible_disable_legacy_crosstool_fields, please "
            + "migrate your CROSSTOOL");
  }

  @Test
  public void testDisablingExpandIfAllAvailable() throws Exception {
    reporter.removeHandler(failFastHandler);
    AnalysisMock.get()
        .ccSupport()
        .setupCrosstool(
            mockToolsConfig,
            "feature { ",
            "  name: 'foo'",
            "  flag_set { expand_if_all_available: 'bar' }",
            "}");

    scratch.file("a/BUILD", "cc_library(name='a', srcs=['a.cc'])");

    useConfiguration(
        "--noincompatible_disable_expand_if_all_available_in_flag_set",
        "--noincompatible_disable_crosstool_file");
    getConfiguredTarget("//a");
    assertNoEvents();

    useConfiguration(
        "--incompatible_disable_expand_if_all_available_in_flag_set",
        "--noincompatible_disable_crosstool_file");
    getConfiguredTarget("//a");

    assertContainsEvent(
        "defines a flag_set with expand_if_all_available set. This is disabled by "
            + "--incompatible_disable_expand_if_all_available_in_flag_set");
  }

  /*
   * Crosstools should load fine with or without 'gcov-tool'. Those that define 'gcov-tool'
   * should also add a make variable.
   */
  @Test
  public void testGcovToolNotDefined() throws Exception {
    // Crosstool with gcov-tool
    scratch.file(
        "a/BUILD",
        "load(':cc_toolchain_config.bzl', 'cc_toolchain_config')",
        "filegroup(",
        "   name='empty')",
        "cc_toolchain_suite(",
        "    name = 'a',",
        "    toolchains = { 'k8': ':b' },",
        ")",
        "cc_toolchain(",
        "    name = 'b',",
        "    cpu = 'banana',",
        "    all_files = ':empty',",
        "    ar_files = ':empty',",
        "    as_files = ':empty',",
        "    compiler_files = ':empty',",
        "    dwp_files = ':empty',",
        "    linker_files = ':empty',",
        "    strip_files = ':empty',",
        "    objcopy_files = ':empty',",
        "    toolchain_identifier = 'banana',",
        "    toolchain_config = ':k8-compiler_config',",
        ")",
        CcToolchainConfig.builder()
            .withToolPaths(
                Pair.of("gcc", "path-to-gcc-tool"),
                Pair.of("ar", "ar"),
                Pair.of("cpp", "cpp"),
                Pair.of("gcov", "gcov"),
                Pair.of("ld", "ld"),
                Pair.of("nm", "nm"),
                Pair.of("objdump", "objdump"),
                Pair.of("strip", "strip"))
            .build()
            .getCcToolchainConfigRule());
    analysisMock.ccSupport().setupCcToolchainConfig(mockToolsConfig, CcToolchainConfig.builder());
    mockToolsConfig.create(
        "a/cc_toolchain_config.bzl",
        ResourceLoader.readFromResources(
            "com/google/devtools/build/lib/analysis/mock/cc_toolchain_config.bzl"));
    useConfiguration("--cpu=k8", "--host_cpu=k8");
    CcToolchainProvider ccToolchainProvider =
        (CcToolchainProvider) getConfiguredTarget("//a:a").get(ToolchainInfo.PROVIDER);
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
    ccToolchainProvider.addGlobalMakeVariables(builder);
    assertThat(builder.build().get("GCOVTOOL")).isNull();
  }

  @Test
  public void testGcovToolDefined() throws Exception {
    // Crosstool with gcov-tool
    scratch.file(
        "a/BUILD",
        "load(':cc_toolchain_config.bzl', 'cc_toolchain_config')",
        "filegroup(",
        "   name='empty')",
        "cc_toolchain_suite(",
        "    name = 'a',",
        "    toolchains = { 'k8': ':b' },",
        ")",
        "cc_toolchain(",
        "    name = 'b',",
        "    cpu = 'banana',",
        "    all_files = ':empty',",
        "    ar_files = ':empty',",
        "    as_files = ':empty',",
        "    compiler_files = ':empty',",
        "    dwp_files = ':empty',",
        "    linker_files = ':empty',",
        "    strip_files = ':empty',",
        "    objcopy_files = ':empty',",
        "    toolchain_identifier = 'banana',",
        "    toolchain_config = ':k8-compiler_config',",
        ")",
        CcToolchainConfig.builder()
            .withToolPaths(
                Pair.of("gcc", "path-to-gcc-tool"),
                Pair.of("gcov-tool", "path-to-gcov-tool"),
                Pair.of("ar", "ar"),
                Pair.of("cpp", "cpp"),
                Pair.of("gcov", "gcov"),
                Pair.of("ld", "ld"),
                Pair.of("nm", "nm"),
                Pair.of("objdump", "objdump"),
                Pair.of("strip", "strip"))
            .build()
            .getCcToolchainConfigRule());
    analysisMock.ccSupport().setupCcToolchainConfig(mockToolsConfig, CcToolchainConfig.builder());
    mockToolsConfig.create(
        "a/cc_toolchain_config.bzl",
        ResourceLoader.readFromResources(
            "com/google/devtools/build/lib/analysis/mock/cc_toolchain_config.bzl"));
    useConfiguration("--cpu=k8", "--host_cpu=k8");
    CcToolchainProvider ccToolchainProvider =
        (CcToolchainProvider) getConfiguredTarget("//a:a").get(ToolchainInfo.PROVIDER);
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
    ccToolchainProvider.addGlobalMakeVariables(builder);
    assertThat(builder.build().get("GCOVTOOL")).isNotNull();
  }

  @Test
  public void testUnsupportedSysrootErrorMessage() throws Exception {
    scratch.file(
        "a/BUILD",
        "load(':cc_toolchain_config.bzl', 'cc_toolchain_config')",
        "filegroup(name='empty')",
        "filegroup(name='everything')",
        "cc_toolchain_suite(",
        "    name = 'a',",
        "    toolchains = { 'k8': ':b' },",
        ")",
        "cc_toolchain(",
        "    name = 'b',",
        "    cpu = 'banana',",
        "    all_files = ':empty',",
        "    ar_files = ':empty',",
        "    as_files = ':empty',",
        "    compiler_files = ':empty',",
        "    dwp_files = ':empty',",
        "    linker_files = ':empty',",
        "    strip_files = ':empty',",
        "    objcopy_files = ':empty',",
        "    toolchain_identifier = 'banana',",
        "    toolchain_config = ':k8-compiler_config',",
        ")",
        // Not specifying `builtin_sysroot` means the toolchain doesn't support --grte_top.
        CcToolchainConfig.builder().withSysroot("").build().getCcToolchainConfigRule());
    analysisMock.ccSupport().setupCcToolchainConfig(mockToolsConfig, CcToolchainConfig.builder());
    mockToolsConfig.create(
        "a/cc_toolchain_config.bzl",
        ResourceLoader.readFromResources(
            "com/google/devtools/build/lib/analysis/mock/cc_toolchain_config.bzl"));
    reporter.removeHandler(failFastHandler);
    useConfiguration("--grte_top=//a", "--cpu=k8", "--host_cpu=k8");
    getConfiguredTarget("//a:a");
    assertContainsEvent("does not support setting --grte_top");
  }

  @Test
  public void testConfigWithMissingToolDefs() throws Exception {
    scratch.file(
        "a/BUILD",
        "load(':cc_toolchain_config.bzl', 'cc_toolchain_config')",
        "filegroup(",
        "   name='empty')",
        "cc_toolchain_suite(",
        "    name = 'a',",
        "    toolchains = { 'k8': ':b' },",
        ")",
        "cc_toolchain(",
        "    name = 'b',",
        "    cpu = 'banana',",
        "    all_files = ':empty',",
        "    ar_files = ':empty',",
        "    as_files = ':empty',",
        "    compiler_files = ':empty',",
        "    dwp_files = ':empty',",
        "    linker_files = ':empty',",
        "    strip_files = ':empty',",
        "    objcopy_files = ':empty',",
        "    toolchain_identifier = 'banana',",
        "    toolchain_config = ':k8-compiler_config',",
        ")",
        CcToolchainConfig.builder()
            .withToolPaths(
                Pair.of("gcc", "path-to-gcc-tool"),
                Pair.of("ar", "ar"),
                Pair.of("cpp", "cpp"),
                Pair.of("gcov", "gcov"),
                Pair.of("ld", "ld"),
                Pair.of("nm", "nm"),
                Pair.of("objdump", "objdump")
                // Pair.of("strip", "strip")
                )
            .build()
            .getCcToolchainConfigRule());
    analysisMock.ccSupport().setupCcToolchainConfig(mockToolsConfig, CcToolchainConfig.builder());
    mockToolsConfig.create(
        "a/cc_toolchain_config.bzl",
        ResourceLoader.readFromResources(
            "com/google/devtools/build/lib/analysis/mock/cc_toolchain_config.bzl"));
    reporter.removeHandler(failFastHandler);
    useConfiguration("--cpu=k8", "--host_cpu=k8");
    getConfiguredTarget("//a:a");
    assertContainsEvent("Tool path for 'strip' is missing");
  }

  /** For a fission-supporting crosstool: check the dwp tool path. */
  @Test
  public void testFissionConfigWithMissingDwp() throws Exception {
    scratch.file(
        "a/BUILD",
        "filegroup(name='empty') ",
        "cc_toolchain_suite(",
        "    name = 'a',",
        "    toolchains = { 'k8': ':b' },",
        ")",
        "cc_toolchain(",
        "    name = 'b',",
        "    cpu = 'banana',",
        "    all_files = ':empty',",
        "    ar_files = ':empty',",
        "    as_files = ':empty',",
        "    compiler_files = ':empty',",
        "    dwp_files = ':empty',",
        "    linker_files = ':empty',",
        "    strip_files = ':empty',",
        "    objcopy_files = ':empty',",
        "    proto = \"\"\"",
        "      toolchain_identifier: \"a\"",
        "      host_system_name: \"a\"",
        "      target_system_name: \"a\"",
        "      target_cpu: \"a\"",
        "      target_libc: \"a\"",
        "      compiler: \"a\"",
        "      abi_version: \"a\"",
        "      abi_libc_version: \"a\"",
        "      supports_fission: 1",
        "      tool_path { name: 'gcc' path: 'path-to-gcc-tool' }",
        "      tool_path { name: 'ar' path: 'ar' }",
        "      tool_path { name: 'cpp' path: 'cpp' }",
        "      tool_path { name: 'gcov' path: 'gcov' }",
        "      tool_path { name: 'ld' path: 'ld' }",
        "      tool_path { name: 'nm' path: 'nm' }",
        "      tool_path { name: 'objdump' path: 'objdump' }",
        "      tool_path { name: 'strip' path: 'strip' }",
        "\"\"\")");
    reporter.removeHandler(failFastHandler);
    analysisMock.ccSupport().setupCrosstool(mockToolsConfig);
    useConfiguration("--cpu=k8", "--host_cpu=k8", "--noincompatible_disable_crosstool_file");
    getConfiguredTarget("//a:a");
    assertContainsEvent("Tool path for 'dwp' is missing");
  }

  @Test
  public void testRuntimeLibsAttributesAreNotObligatory() throws Exception {
    scratch.file(
        "a/BUILD",
        "load(':cc_toolchain_config.bzl', 'cc_toolchain_config')",
        "filegroup(name='empty') ",
        "cc_toolchain_suite(",
        "    name = 'a',",
        "    toolchains = { 'k8': ':b' },",
        ")",
        "cc_toolchain(",
        "    name = 'b',",
        "    cpu = 'banana',",
        "    all_files = ':empty',",
        "    ar_files = ':empty',",
        "    as_files = ':empty',",
        "    compiler_files = ':empty',",
        "    dwp_files = ':empty',",
        "    linker_files = ':empty',",
        "    strip_files = ':empty',",
        "    objcopy_files = ':empty',",
        "    toolchain_identifier = 'banana',",
        "    toolchain_config = ':banana_config',",
        ")",
        "cc_toolchain_config(name = 'banana_config')");
    scratch.file("a/cc_toolchain_config.bzl", MockCcSupport.EMPTY_CC_TOOLCHAIN);
    analysisMock.ccSupport().setupCcToolchainConfig(mockToolsConfig, CcToolchainConfig.builder());
    reporter.removeHandler(failFastHandler);
    useConfiguration("--cpu=k8", "--host_cpu=k8");
    getConfiguredTarget("//a:a");
    assertNoEvents();
  }

  @Test
  public void testWhenStaticRuntimeLibAttributeMandatoryWhenSupportsEmbeddedRuntimes()
      throws Exception {
    scratch.file(
        "a/BUILD",
        "load(':cc_toolchain_config.bzl', 'cc_toolchain_config')",
        "filegroup(name = 'empty')",
        "cc_binary(name = 'main', srcs = [ 'main.cc' ],)",
        "cc_binary(name = 'test', linkstatic = 0, srcs = [ 'test.cc' ],)",
        "cc_toolchain_suite(",
        "    name = 'a',",
        "    toolchains = { 'k8': ':b'},",
        ")",
        "cc_toolchain(",
        "    name = 'b',",
        "    cpu = 'banana',",
        "    all_files = ':empty',",
        "    ar_files = ':empty',",
        "    as_files = ':empty',",
        "    compiler_files = ':empty',",
        "    dwp_files = ':empty',",
        "    linker_files = ':empty',",
        "    strip_files = ':empty',",
        "    objcopy_files = ':empty',",
        "    toolchain_identifier = 'banana',",
        "    toolchain_config = ':k8-compiler_config',",
        ")",
        CcToolchainConfig.builder()
            .withFeatures(CppRuleClasses.STATIC_LINK_CPP_RUNTIMES)
            .build()
            .getCcToolchainConfigRule());
    analysisMock.ccSupport().setupCcToolchainConfig(mockToolsConfig, CcToolchainConfig.builder());
    mockToolsConfig.create(
        "a/cc_toolchain_config.bzl",
        ResourceLoader.readFromResources(
            "com/google/devtools/build/lib/analysis/mock/cc_toolchain_config.bzl"));
    reporter.removeHandler(failFastHandler);
    useConfiguration(
        "--crosstool_top=//a:a",
        "--cpu=k8",
        "--host_cpu=k8",
        "--experimental_disable_legacy_crosstool_fields");
    assertThat(getConfiguredTarget("//a:main")).isNull();
    assertContainsEvent(
        "Toolchain supports embedded runtimes, but didn't provide static_runtime_lib attribute.");
  }

  @Test
  public void testWhenDynamicRuntimeLibAttributeMandatoryWhenSupportsEmbeddedRuntimes()
      throws Exception {
    scratch.file(
        "a/BUILD",
        "load(':cc_toolchain_config.bzl', 'cc_toolchain_config')",
        "filegroup(name = 'empty')",
        "cc_binary(name = 'main', srcs = [ 'main.cc' ],)",
        "cc_binary(name = 'test', linkstatic = 0, srcs = [ 'test.cc' ],)",
        "cc_toolchain_suite(",
        "    name = 'a',",
        "    toolchains = { 'k8': ':b'},",
        ")",
        "cc_toolchain(",
        "    name = 'b',",
        "    cpu = 'banana',",
        "    all_files = ':empty',",
        "    ar_files = ':empty',",
        "    as_files = ':empty',",
        "    compiler_files = ':empty',",
        "    dwp_files = ':empty',",
        "    linker_files = ':empty',",
        "    strip_files = ':empty',",
        "    objcopy_files = ':empty',",
        "    static_runtime_lib = ':empty',",
        "    toolchain_identifier = 'banana',",
        "    toolchain_config = ':k8-compiler_config',",
        ")",
        CcToolchainConfig.builder()
            .withFeatures(
                CppRuleClasses.STATIC_LINK_CPP_RUNTIMES, CppRuleClasses.SUPPORTS_DYNAMIC_LINKER)
            .build()
            .getCcToolchainConfigRule());
    analysisMock.ccSupport().setupCcToolchainConfig(mockToolsConfig, CcToolchainConfig.builder());
    mockToolsConfig.create(
        "a/cc_toolchain_config.bzl",
        ResourceLoader.readFromResources(
            "com/google/devtools/build/lib/analysis/mock/cc_toolchain_config.bzl"));
    reporter.removeHandler(failFastHandler);
    useConfiguration(
        "--crosstool_top=//a:a",
        "--cpu=k8",
        "--host_cpu=k8",
        "--dynamic_mode=fully",
        "--experimental_disable_legacy_crosstool_fields");
    assertThat(getConfiguredTarget("//a:test")).isNull();
    assertContainsEvent(
        "Toolchain supports embedded runtimes, but didn't provide dynamic_runtime_lib attribute.");
  }
}
