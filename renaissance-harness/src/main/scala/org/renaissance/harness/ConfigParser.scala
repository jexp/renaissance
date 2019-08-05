package org.renaissance.harness

import scopt.OptionParser

final class ConfigParser(tags: Map[String, String]) {

  private val parser = createParser(tags)

  private def createParser(tags: Map[String, String]) = {
    new OptionParser[Config]("renaissance") {
      head(s"${tags("renaissanceTitle")}, version ${tags("renaissanceVersion")}")

      help('h', "help")
        .text("Prints this usage text.")
      opt[Int]('r', "repetitions")
        .text("Execute the measured operation a fixed number of times.")
        .action((v, c) => c.withRepetitions(v))
      opt[Int]('t', "run-seconds")
        .text("Execute the measured operation for a fixed number of seconds (wall-clock time).")
        .action((v, c) => c.withWallClockRunSeconds(v))
      opt[Int]("operation-run-seconds")
        .text(
          "Execute the measured operation for a fixed number of seconds (net operation time)."
        )
        .action((v, c) => c.withOperationRunSeconds(v))
      opt[String]("policy")
        .text(
          "Use external policy to control repetitions, specified as <class-path>!<class-name>."
        )
        .validate(
          v =>
            if (v.count(_ == '!') == 1) success
            else failure("expected <class-path>!<class-name> in external policy specification")
        )
        .action((v, c) => c.withPolicy(v))
      opt[String]("plugin")
        .text(
          "Load external plugin, specified as <classpath>!<class-name>. Can appear multiple times."
        )
        .action((v, c) => c.withPlugin(v))
        .validate(
          v =>
            if (v.count(_ == '!') == 1) success
            else failure("expected <class-path>!<class-name> in external plugin specification")
        )
        .unbounded()
      opt[String]("csv")
        .text("Output results to CSV file.")
        .action((v, c) => c.withCsvOutput(v))
      opt[String]("json")
        .text("Output results to JSON file.")
        .action((v, c) => c.withJsonOutput(v))
      opt[String]('c', "configuration")
        .text("Run benchmarks with given named configuration.")
        .action((v, c) => c.withConfiguration(v))
      opt[Unit]("list")
        .text("Print list of benchmarks with their description.")
        .action((_, c) => c.withList())
      opt[Unit]("raw-list")
        .text("Print list of benchmarks (each benchmark name on separate line).")
        .action((_, c) => c.withRawList())
      opt[Unit]("group-list")
        .text("Print list of benchmark groups (each group name on separate line).")
        .action((_, c) => c.withGroupList())
      arg[String]("benchmark-specification")
        .text("Comma-separated list of benchmarks (or groups) that must be executed (or all).")
        .action((v, c) => c.withBenchmarkSpecification(v))
        .unbounded()
        .optional()
    }
  }

  def parse(args: Array[String]): Option[Config] = parser.parse(args, new Config)

  def usage(): String = parser.usage

}
