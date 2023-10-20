task echo {
    String name
    command {
        echo "Hello ${name}"
        >&2 echo "Goodbye ${name}"
        echo "Bye" > "test.txt"
        echo "Bye" > "test2.txt"
    }

    runtime {
        docker: "ubuntu"
    }

    output {
        File out = stdout()
        File out2 = "test.txt"
        Array[File] arrayOut = [out, out2, "test2.txt"]
    }
}

workflow hello_world {
    String name
    call echo {
        input:
            name = name
    }
    output {
        File out = echo.out
    }
}