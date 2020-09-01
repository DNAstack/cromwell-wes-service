
task echo {
  String name
  command {
    echo "Hello ${name}"
    >&2 echo "Goodbye ${name}"
  }

  runtime {
    docker: "ubuntu"
  }

  output {
    File out = stdout()
  }
}

workflow hello_world {
  String name
  call echo { input: name = name }
  output {
    File out = echo.out
  }
}