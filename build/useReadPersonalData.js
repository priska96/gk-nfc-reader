import { useEffect, useState } from "react";
import { readPersonalDataNative, eventEmitter, State, } from "./RNGKNFCReader";
export const useReadPersonalData = () => {
    const [result, setResult] = useState("");
    const [state, setState] = useState({
        state: State.idle,
        value: undefined,
        error: undefined,
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
            .then((res) => {
            setResult(res); // Store the result in state
            console.log("success");
        })
            .catch((error) => {
            console.log("error", error);
        });
    };
    return { result, state, readPersonalData };
};
//# sourceMappingURL=useReadPersonalData.js.map