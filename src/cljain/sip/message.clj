(ns ^{:doc "place doc string here"
      :author "ruiyun"}
  cljain.sip.message
  (:use     cljain.tools.predicate
            [cljain.sip.core :only [sip-factory]])
  (:require [cljain.sip.address :as addr]
            [cljain.sip.header :as header])
  (:import  [javax.sip SipFactory]
            [javax.sip.message Message MessageFactory Request Response]))

(def ^{:doc "The factory to create Request and Response."
       :added "0.2.0"
       :private true}
  factory (.createMessageFactory sip-factory))

(defn request
  "Creates a new Request message of type specified by the method paramater,
  containing the URI of the Request, the mandatory headers of the message.
  This new Request does not contain a body."
  {:added "0.2.0"}
  [method req-uri from call-id & more-headers]
  (let [headers (apply hash-map (mapcat #(vector (.getName %) %) (remove nil? (flatten more-headers))))
        c-seq (get headers "CSeq" (header/c-seq 1 method))
        to (get headers "To" (header/to (addr/address req-uri) nil))
        via (remove nil? [(get headers "Via")])
        max-forward (get headers "Max-Forwards" (header/max-forwards 70))
        request (.createRequest factory req-uri method call-id c-seq from to via max-forward)
        remain-headers (remove #(in? % ["From" "Call-ID" "CSeq" "To" "Via" "Max-Forwards"]) (keys headers))
        remain-headers (vals (select-keys headers remain-headers))]
    (doseq [h remain-headers] (.setHeader request h))
    request))

(defn response
  "Creates a new Response message of type specified by the statusCode paramater,
  based on a specific Request message. This new Response does not contain a body.
  Only the required headers are copied from the Request."
  {:added "0.2.0"}
  [status-code, ^Request reqest & more-headers]
  {:pre [(let [headers (remove nil? (flatten more-headers))]
           (or (= 0 (count headers)) (every? header/header? headers)))]}
  (let [response (.createResponse factory status-code reqest)
        headers (remove nil? (flatten more-headers))]
    (doseq [h headers] (.setHeader response h))
    response))

(defn header
  "Gets the Header of the specified name in this Message.
  If multiple Headers of this header name exist in the message,
  the first header in the message is returned."
  {:added "0.2.0"}
  [^Message message, header-name]
  (.getHeader message header-name))

(defn set-header!
  "Sets the new Header to replace existings Header of that type in the message.
  If the SIP message contains more than one Header of the new Header type it should
  replace the first occurance of this Header and removes all other Headers of this type.
  If no Header of this type exists this header is added to the end of the SIP Message.
  This method should be used to change required Headers and overwrite optional Headers."
  {:added "0.2.0"}
  [^Message message, header]
  (.setHeader message header)
  message)

(defn add-header!
  "The Header is added to the end of the List and will appear in that order in the SIP Message.
  Required Headers that are singletons should not be added to the message as they already
  exist in the message and therefore should be changed using the 'set-header!' method.

  This method should be used to support the special case of adding required ViaHeaders to a message.
  When adding a ViaHeader using this method the implementation will add the ViaHeader to the
  top of the ViaHeader list, and not the end like all other Headers."
  {:added "0.2.0"}
  [^Message message, header]
  (.addHeader message header)
  message)

(defn remove-header!
  "Removes the Header of the supplied name from the list of headers in this Message.
  If multiple headers exist then they are all removed from the header list.
  If no headers exist then this method returns silently.
  This method should not be used to remove required Headers, required Headers should be
  replaced using the 'set-header!'."
  {:added "0.2.0"}
  [^Message message, header-name]
  (.removeHeader message header-name)
  message)

(defn set-content!
  "Sets the new Header to replace existings Header of that type in the message.
  If the SIP message contains more than one Header of the new Header type it should
  replace the first occurance of this Header and removes all other Headers of this type.
  If no Header of this type exists this header is added to the end of the SIP Message.
  This method should be used to change required Headers and overwrite optional Headers."
  {:added "0.2.0"}
  [^Message message, type-header, content]
  (.setContent message content type-header)
  message)

(defn method
  "Gets method string of this Request message."
  {:added "0.2.0"}
  [^Request request]
  (.getMethod request))

(defn status-code
  "Gets the integer value of the status code of Response,
  which identifies the outcome of the request to which this response is related."
  {:added "0.2.0"}
  [^Response response]
  (.getStatusCode response))

(defn reason
  "Gets the reason phrase of this Response message."
  {:added "0.2.0"}
  [^Response response]
  (str (status-code response) \space (.getReasonPhrase response)))

(defn inc-sequence-number!
  "Increase the sequence number of a request's CSeq header."
  {:added "0.3.0"}
  [^Request request]
  (let [sequence-number   (inc (header/sequence-number (header request "CSeq")))
        new-c-seq-header  (header/c-seq sequence-number (method request))]
    (set-header! request new-c-seq-header)
    request))
