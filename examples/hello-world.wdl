version 1.0

task say_hello {
    command <<<
      echo "Hello, World!"
    >>>

    output {
      String greeting = read_string(stdout())
    }

    runtime {
      docker: "ubuntu:latest"
    }
}

workflow hello_world {
  call say_hello
  output {
    String greeting = say_hello.greeting
  }
}