(ns es.main
  (:gen-class)
  (:require [clojure.tools.cli :refer [cli]]
            [bultitude.core :refer [namespaces-on-classpath]]
            [es.format.table :refer [tabler]]
            [es.command :as comm]
            [slingshot.slingshot :refer [try+]]
            ))

(def opts
  [["-u" "--url" "ES instance locator" :default "http://localhost:9200"]
   ["-o" "--output" "Output format [only raw right now]" :default :raw]
   ["-v" "--[no-]verbose" :default false]])

(defn find-command [ns var]
  (let [var (symbol (format "%s/%s" ns var))]
    (try
      (find-var var)
      (catch Exception _))))

(defn help-commands []
  (println "Available commands:")
  (println)
  (print "  ")
  (println (->> comm/available
                (interpose "\n  " )
                (apply str))))

(defn error [fmt & args]
  (binding [*out* *err*]
    (apply printf (str (.trim fmt) "\n") args)
    (flush)))

(defn help [bann]
  (println "Usage: es COMMAND [OPTS]")
  (println)
  (println (.replace bann "Usage:\n\n" ""))
  (println)
  (help-commands))

(defn die [banner fmt & args]
  (apply error fmt args)
  (help banner)
  (System/exit 99))

(defn main [cmd args opts]
  (let [cmd (find-command
             (symbol (format "es.command.%s" cmd))
             'go)]
    (if cmd
      (cmd args opts)
      :fail)))

(defn -main [& args]
  (let [[opts args banner] (apply cli args opts)]
    (let [[cmd & args] args
          res (try+
                (main cmd args opts)
                (catch [:type :es.http/error] {:keys [msg]}
                  (error msg))
                (catch Object _
                  (error "unexpected: %s" &throw-context)))]
      (condp = res
        :fail (if cmd
                (die banner "no command %s" cmd))
        (tabler opts res)))))
