package com.dnastack.wes.service.utils;

public class WdlSupplier {

    public static final String WORKFLOW_WITHOUT_FILE = "\n"
        + "task echo {\n"
        + "\n"
        + "\n"
        + "  String name\n"
        + "\n"
        + "  command {\n"
        + "    ehco Hello ${name}\n"
        + "  }\n"
        + "\n"
        + "  output {\n"
        + "    File out = stdout()\n"
        + "\n"
        + "  }\n"
        + "}\n"
        + "\n"
        + "workflow hello_world {\n"
        + "  String name\n"
        + "  call echo {\n"
        + "    input: name = name\n"
        + "  }\n"
        + "\n"
        + "  output {\n"
        + "    File out = echo.out\n"
        + "  }\n"
        + "}";


    public static final String ECHO_WITH_IMPORT_WDL = "version 1.0 \n"
        + "\n"
        + "import \"struct_test.wdl\" as struct_test\n"
        + "\n"
        + "task echo { \n"
        + "  input { \n"
        + "    String s\n"
        + "  } \n"
        + "  command { \n"
        + "    echo ~{s}  \n"
        + "  }\n"
        + "  output{\n"
        + "    String c = read_string(stdout())\n"
        + "  } \n"
        + "  runtime { \n"
        + "    docker: \"ubuntu:latest\"\n"
        + "  }\n"
        + "} \n"
        + "\n"
        + "workflow x { \n"
        + "  input {\n"
        + "    Parent p\n"
        + "    String s\n"
        + "  } \n"
        + "  call echo {\n"
        + "    input: s = p.name\n"
        + "  }\n"
        + "  \n"
        + "  call echo as echo2 {\n"
        + "    input: s = p.children[0].name\n"
        + "  }\n"
        + "\n"
        + "  call echo as echo3 {\n"
        + "    input: s = read_string(p.info)\n"
        + "  }\n"
        + "\n"
        + "  call echo as echo4 {\n"
        + "    input: s = s\n"
        + "  }\n"
        + "\n"
        + "\n"
        + "  output {\n"
        + "    String d = echo.c\n"
        + "    String d2 = echo2.c\n"
        + "    String d3 = echo3.c\n"
        + "    String d4 = echo4.c\n"
        + "  }\n"
        + "}";


    public static final String STRUCT_TEST_WDL = "version 1.0\n"
        + "\n"
        + "struct Child {\n"
        + "    Int age\n"
        + "    String name\n"
        + "}\n"
        + "\n"
        + "struct Parent {\n"
        + "  Int age\n"
        + "  String name\n"
        + "  File info\n"
        + "  Array[Child] children\n"
        + "}\n";

    public static final String ECHO_WITH_IMPORT_INPUTS = "{\n"
        + "  \"x.s\":\"ggggggg\",\n"
        + "  \"x.p\":{\n"
        + "    \"name\": \"jonas\",\n"
        + "    \"age\": 42,\n"
        + "    \"info\": {\n"
        + "      \"id\": \"1-87-99\",\n"
        + "      \"created\":\"\",\n"
        + "      \"size\": 12123,\n"
        + "      \"check_sums\":[\n"
        + "        {\n"
        + "          \"checksum\":\"1urM1pz5IYA+ZusrZ98CLw==\",\n"
        + "          \"type\":\"md5\"\n"
        + "        }\n"
        + "      ],\n"
        + "      \"access_methods\":[\n"
        + "        {\n"
        + "          \"access_url\":{\n"
        + "           \"url\": \"/home/patrick/development/dnastack/wdl-validator/scripts/wdl/string.txt\"\n"
        + "          },\n"
        + "          \"type\": \"file\"\n"
        + "        },\n"
        + "        {\n"
        + "          \"access_url\":{\n"
        + "            \"url\":\"gs://genomics-public-data/references/README\"\n"
        + "          },\n"
        + "          \"type\":\"gs\"\n"
        + "        }\n"
        + "      ]\n"
        + "    },\n"
        + "    \"children\": [{\n"
        + "      \"name\": \"charlie\",\n"
        + "      \"age\": 7\n"
        + "    }]\n"
        + "  }\n"
        + "}\n";

    public static final String MD5_SUM_WDL = "version 1.0\n"
            + "\n"
            + "workflow md5Sum {\n"
            + "input {\n"
            + "File inputFile\n"
            + "}\n"
            + "\n"
            + "call calculateMd5Sum {\n"
            + "input:\n"
            + "inputFile = inputFile\n"
            + "}\n"
            + "\n"
            + "output {\n"
            + "String md5 = calculateMd5Sum.md5\n"
            + "}\n"
            + "}\n"
            + "\n"
            + "task calculateMd5Sum {\n"
            + "input {\n"
            + "File inputFile\n"
            + "String outname = basename(inputFile) + \".md5\"\n"
            + "}\n"
            + "\n"
            + "Int diskSize = ceil(size(inputFile,\"GB\")) + 15\n"
            + "\n"
            + "command <<<\n"
            + "gsutil hash -m ~{inputFile} | grep -E 'Hash (md5):s' | cut -f4 > ~{outname}\n"
            + ">>>\n"
            + "\n"
            + "output {\n"
            + "String md5 = read_string(\"${outname}\")\n"
            + "}\n"
            + "\n"
            + "runtime {\n"
            + "docker: \"google/cloud-sdk:slim\"\n"
            + "cpu: 1\n"
            + "memory: \"3.75 GB\"\n"
            + "disks: \"local-disk \" + diskSize + \" HDD\"\n"
            + "}\n"
            + "}\n";
}
