version 1.0
workflow test {
  input {
    File input_file
  }

  call say_hello as normal_say_hello {
    input:
      input_file = input_file
  }
  output {
    String o = normal_say_hello.out
  }
}
task say_hello {
  input {
    File input_file
  }
  output {
    String out = read_string(stdout())
  }
  command <<<
    cat ~{input_file}
  >>>
  runtime {
    docker: "debian:bullseye-slim"
  }
}
