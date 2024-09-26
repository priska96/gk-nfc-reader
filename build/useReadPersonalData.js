import { useEffect, useState } from "react";
import { readPersonalDataNative, eventEmitter, } from "./RNNFCLoginController";
export const useReadPersonalData = () => {
    const [res, setRes] = useState("");
    const [state, setState] = useState({
        state: "idle",
        value: false,
        error: "",
    });
    useEffect(() => {
        if (!eventEmitter)
            return;
        // Subscribe to the event emitter
        const subscription = eventEmitter.addListener("onStatusChange", (event) => {
            console.log("State updated:", event);
            setState(event); // Update the local state with the received event
        });
        // Clean up the subscription on component unmount
        return () => {
            subscription.remove();
        };
    }, [eventEmitter]);
    const readPersonalData = ({ can, pin, checkBrainpoolAlgorithm, }) => {
        readPersonalDataNative({
            can,
            pin,
            checkBrainpoolAlgorithm,
        })
            .then((result) => {
            setRes(result); // Store the result in state
            console.log("success");
        })
            .catch((error) => {
            console.error("error", error);
        });
    };
    return { res, state, readPersonalData };
};
//# sourceMappingURL=useReadPersonalData.js.map