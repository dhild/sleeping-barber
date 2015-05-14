(ns sleeping-barber.core
  (:gen-class))

(def customer-min-time 10)
(def customer-max-time 30)
(def haircut-time 20)
(def waiting-room-size 3)

(defn wait-for-customer-time []
  (+ customer-min-time (rand-int (inc (- customer-max-time customer-min-time)))))

(defn send-in-customers
  "Sends customers to the waiting room as long as the shop is open."
  [shop-open? waiting-room lost-customers]
  (future
    (println "Sending in customers.")
    (while @shop-open?
      (dosync
       (if (< (count @waiting-room) waiting-room-size)
         (alter waiting-room conj :customer)
         (send lost-customers inc)))
       (Thread/sleep (wait-for-customer-time)))
    (println "No more customers, shop closed!")))

(defn do-barbery
  "Cuts hair as long as there are customers."
  [shop-open? waiting-room haircut-count]
  (future
    (println "Cutting hair")
    (while @shop-open?
      (if (> (count @waiting-room) 0)
        (do
          (dosync (alter waiting-room rest))
          (Thread/sleep haircut-time)
          (send haircut-count inc))))
    (println "Done cutting hair")))

(defn open-shop
  "Opens the shop, telling the barber to start working and the customers to come in."
  [shop-open?]
  (let [haircut-count (agent 0)
        lost-customers (agent 0)
        waiting-room (ref [])
        barber (do-barbery shop-open? waiting-room haircut-count)
        customers (send-in-customers shop-open? waiting-room lost-customers)]
    (future
      @barber
      @customers
      {:haircuts @haircut-count
       :lost-customers @lost-customers})))

(defn run-barbershop
  "Runs the barbershop for the given time and returns the results."
  [open-time]
  (let [shop-open? (atom true)
        values (open-shop shop-open?)]
    (Thread/sleep open-time)
    (println "Closing shop...")
    (swap! shop-open? not)
    @values))

(defn -main
  "Runs the barbershop for 10 seconds and prints the results."
  [& args]
  (do
    (println (run-barbershop (* 10 1000)))
    (System/exit 0)))
