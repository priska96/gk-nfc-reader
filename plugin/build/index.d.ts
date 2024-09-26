import { ConfigPlugin } from "expo/config-plugins";
declare const withGKNFCReader: ConfigPlugin<{
    nfcReaderUsageDescription: string;
    deploymentTarget: string;
}>;
export default withGKNFCReader;
