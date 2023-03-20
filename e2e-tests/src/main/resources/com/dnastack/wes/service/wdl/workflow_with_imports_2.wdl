version 1.0

struct Child {
    Int age
    String name
}

struct Parent {
    Int age
    String name
    Array[Child] children
}


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


workflow greet_family {
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
                s = p.name
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