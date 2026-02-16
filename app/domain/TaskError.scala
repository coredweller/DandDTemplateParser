package domain

// Scala 3 enum — no sealed trait needed for simple ADTs
enum TaskError:
  case NotFound(id: TaskId)
  case ValidationError(message: String)
