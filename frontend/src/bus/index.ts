import mitt from "mitt";
import messages from './messages';
const bus: any = {};
const emitter = mitt();
bus.on = emitter.on;
bus.off = emitter.off;
bus.emit = emitter.emit;
messages.install(bus)
export default bus;
