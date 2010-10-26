(ns am.ik.categolj.daccess.mirage.daccess
  (:use [am.ik.categolj.utils date-utils])
  (:use [am.ik.categolj.daccess daccess entities])
  (:use [clojure.contrib singleton])
  (:require [clojure.contrib.logging :as log])
  (:import [am.ik.categolj.daccess.entities Entry Category User])
  (:import [am.ik.categolj.daccess.mirage.entities EntryEntity])
  (:import [jp.sf.amateras.mirage SqlManager])
  (:import [jp.sf.amateras.mirage.session Session JDBCSessionImpl])
  (:import [jp.sf.amateras.mirage.exception SQLRuntimeException]))


(defmacro with-tx [[^Session session] & expr]
  `(do
     (.begin ~session)
     (try
       (let [ret# (do ~@expr)]
         (.commit ~session)                  
         ret#)
       (catch Exception e#
         (log/error "Exception occured in transaction!" e#)
         (.rollback ~session))
       (finally
        (.release ~session)))))

(defn ^Entry entity-to-record [^EntryEntity entity]
  (if entity
    (Entry.
     {}
     {:id (.id entity),
      :title (.title entity)
      :content (.content entity)
      :created-at (.createdAt entity)
      :updated-at (.updatedAt entity)
      :category ["Test" "Sample"] ; stub
      })))

(defn ^EntryEntity record-to-entity [param]
  (if param
    (EntryEntity. (let [id (:id param)]
                    (if (string? id) (Long/valueOf (:id param)) id))
                  (:title param)
                  (:content param)
                  (let [created-at (:created-at param)]
                    (if (string? created-at) (parse-date created-at) created-at))
                  (let [updated-at (:updated-at param)]
                    (if (string? updated-at) (parse-date updated-at) updated-at))
                  ; category ?
                  )))

(defrecord MirageDataAccess [^Session session]
  DataAccess
  (get-entry-by-id 
   [this id]
   (with-tx [session]
     (entity-to-record
      (.getSingleResult (.getSqlManager session)
                        EntryEntity
                        "sql/get-entry-by-id.sql"
                        {"id" id}))))
  
  (get-entries-by-page 
   [this page count]
   (map entity-to-record
        (with-tx [session]
          (.getResultList
           (.getSqlManager session)
           EntryEntity
           "sql/get-entries-by-page.sql"
           {"limit" count, "offset" (* count (dec page))}))))
  
  (get-total-count
   [this]
   (with-tx [session]
     (.getCount
      (.getSqlManager session)
      "sql/get-total-count.sql")))
  
  (update-entry
   [this entry]
   (log/debug "entry" entry)
   (with-tx [session]
     (.executeUpdate
      (.getSqlManager session)
      "sql/update-entry.sql"
      (zipmap (map name (keys entry)) (vals entry)))))
  
  (delete-entry-by-id
   [this id]
   )
  )

;; (def (create-session "org.hsqldb.jdbcDriver" "hsqldb" "mem:categolj" "sa" ""))
(defn ^Session create-session [classname subprotocol subname user password]
  (JDBCSessionImpl. classname (str "jdbc:" subprotocol ":" subname) user password))

(defn create-table
  "create ENTITY table if not exists."
  [^Session session ddl]
  (with-tx [session]    
    (let [^SqlManager manager (.getSqlManager session)]
      (try
        ;; Check whether ENTITY table exists.
        (.getSingleResultBySql (.getSqlManager session) Integer
                               "SELECT COUNT(ID) FROM ENTITY")
        (catch SQLRuntimeException e
          ;; If not exits, then create.
          (.executeUpdateBySql manager ddl)
          ;; Insert test data.
          (.insertEntity manager (EntryEntity. nil "Title1" "hogehoge" (java.util.Date.) (java.util.Date.)))
          (.insertEntity manager (EntryEntity. nil "Title2" "- hogehoge" (java.util.Date.) (java.util.Date.)))
          (.insertEntity manager (EntryEntity. nil "Title3" "`hogehoge`" (java.util.Date.) (java.util.Date.)))
          (.insertEntity manager (EntryEntity. nil "Title4" "### hogehoge" (java.util.Date.) (java.util.Date.)))
          (.insertEntity manager (EntryEntity. nil "Title5" "**hogehoge**" (java.util.Date.) (java.util.Date.)))
          (.insertEntity manager (EntryEntity. nil "Title6" "#### hogehoge" (java.util.Date.) (java.util.Date.)))
          (.insertEntity manager (EntryEntity. nil "Title7" "    hogehoge" (java.util.Date.) (java.util.Date.)))
          )))))


(defn create-daccess [params]
  (let [{:keys [classname subprotocol subname user password]} (:db params)
        session (create-session classname subprotocol subname user password)
        ddl (:ddl params)]
    (create-table session ddl)    
    (MirageDataAccess. session)))

(comment
  ;; A sample of param
  (def param {:db {:classname "org.hsqldb.jdbcDriver"
                   :subprotocol "hsqldb"
                   :subname "mem:categolj"
                   :user "sa"
                   :password ""}
              :ddl "CREATE TABLE ENTITY 
                          (ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) PRIMARY KEY, 
                           TITLE VARCHAR(256), 
                           CONTENT VARCHAR(256), 
                           CREATED_AT TIMESTAMP, 
                           UPDATED_AT TIMESTAMP)"})
  )
