version 1.0

struct Child {
    Int age
    String name
}

struct Parent {
    Int age
    String name
    File info
    Array[Child] children
}
