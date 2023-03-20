version 1.0
import "workflow_with_imports_2.wdl" as import_test


workflow x {
    input {
        Parent p
        String s
    }

    call import_test.greet_family as greet {
        input:
            p = p,
            s = s
    }

    output {
        String d = greet.d
        String d2 = greet.d2
        String d3 = greet.d3
        String d4 = greet.d4
    }
}