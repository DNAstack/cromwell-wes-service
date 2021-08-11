version 1.0

import "workflow_with_imports_2.wdl" as struct_test

task echo {
    input {
        String s
    }
    command {
        echo ~{s}
    }
    output {
        String c = read_string(stdout())
    }
    runtime {
        docker: "ubuntu:latest"
    }
}

workflow x {
    input {
        Parent p
        String s
    }
    call echo {
        input:
            s = p.name
    }

    call echo as echo2 {
        input:
            s = p.children[0].name
    }

    call echo as echo3 {
        input:
            s = read_string(p.info)
    }

    call echo as echo4 {
        input:
            s = s
    }

    output {
        String d = echo.c
        String d2 = echo2.c
        String d3 = echo3.c
        String d4 = echo4.c
    }
}