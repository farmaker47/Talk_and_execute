package com.example.talkandexecute.whisperengine;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import com.example.talkandexecute.utils.WaveUtil;
import com.example.talkandexecute.utils.WhisperUtil;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class WhisperEngine implements IWhisperEngine {
    private final String TAG = "WhisperEngineJava";
    private final WhisperUtil mWhisperUtil = new WhisperUtil();

    private boolean mIsInitialized = false;
    private Interpreter mInterpreter = null;
    private IWhisperListener mUpdateListener = null;
    private final long nativePtr; // Native pointer to the TFLiteEngine instance
    private Context context;

    public WhisperEngine(Context context) {
        nativePtr = createEngine();
        this.context = context;
    }

    @Override
    public boolean isInitialized() {
        return mIsInitialized;
    }

    public void updateStatus(String message) {
        if (mUpdateListener != null)
            mUpdateListener.onUpdateReceived(message);
    }

    public void setUpdateListener(IWhisperListener listener) {
        mUpdateListener = listener;
    }

    @Override
    public boolean initialize(String modelPath, String vocabPath, boolean multilingual) throws IOException {
        // Load model
        loadModel(modelPath);
        Log.d(TAG, "Model is loaded..." + modelPath);

        // Load filters and vocab
        boolean ret = mWhisperUtil.loadFiltersAndVocab(multilingual, vocabPath);
        if (ret) {
            mIsInitialized = true;
            Log.d(TAG, "Filters and Vocab are loaded..." + vocabPath);
        } else {
            mIsInitialized = false;
            Log.d(TAG, "Failed to load Filters and Vocab...");
        }

        return mIsInitialized;
    }

    @Override
    public String transcribeFile(String wavePath) {
        // Calculate Mel spectrogram
        Log.d(TAG, "Calculating Mel spectrogram...");
        long time = System.currentTimeMillis();
        float[] melSpectrogram = getMelSpectrogram(wavePath);
        Log.d(TAG, "Mel spectrogram is calculated...!");
        Log.v("inference_time_mel", String.valueOf(System.currentTimeMillis()-time));

        // Perform inference
        long time2 = System.currentTimeMillis();
        String result = runInference(melSpectrogram);
        Log.d(TAG, "Inference is executed...!");
        Log.v("inference_time_mel", String.valueOf(System.currentTimeMillis()-time2));

        return result;
    }

    private void loadModel(String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        MappedByteBuffer retFile = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        fileDescriptor.close();

        // Set the number of threads for inference
        //Interpreter.Options options = new Interpreter.Options();
        //options.setNumThreads(Runtime.getRuntime().availableProcessors());


        // Initialize interpreter with GPU delegate
        /*Interpreter.Options options = new Interpreter.Options();
        GpuDelegate gpuDelegate = new GpuDelegate();
        options.addDelegate(gpuDelegate);*/

        /*if(compatList.isDelegateSupportedOnThisDevice()){
            // if the device has a supported GPU, add the GPU delegate

        } else {
            // if the GPU is not supported, run on 4 threads
            options.setNumThreads(4);
        }*/
        Interpreter.Options tfliteOptions = new Interpreter.Options();
        tfliteOptions.setNumThreads(Runtime.getRuntime().availableProcessors());
        //tfliteOptions.setUseNNAPI(true);

//        if(compatList.isDelegateSupportedOnThisDevice()){
//            // if the device has a supported GPU, add the GPU delegate
//            //GpuDelegate.Options delegateOptions = compatList.getBestOptionsForThisDevice();
//            //GpuDelegate gpuDelegate = new GpuDelegate();
//            //options.addDelegate(gpuDelegate);
//            options.setNumThreads(10);
//        } else {
//            // if the GPU is not supported, run on 4 threads
//
//        }

        mInterpreter = new Interpreter(retFile, tfliteOptions);
    }

    private float[] getMelSpectrogram(String wavePath) {
        // Get samples in PCM_FLOAT format
        long time = System.currentTimeMillis();
        float[] samples = WaveUtil.getSamples(wavePath);
        Log.v("inference_get_samples", String.valueOf(System.currentTimeMillis()-time));

        int fixedInputSize = WhisperUtil.WHISPER_SAMPLE_RATE * WhisperUtil.WHISPER_CHUNK_SIZE;
        float[] inputSamples = new float[fixedInputSize];
        int copyLength = Math.min(samples.length, fixedInputSize);
        System.arraycopy(samples, 0, inputSamples, 0, copyLength);

        int cores = Runtime.getRuntime().availableProcessors();
        long time2 = System.currentTimeMillis();
        //float[] value = mWhisperUtil.getMultiMelSpectrogram(inputSamples, inputSamples.length, cores);
        float[] value = transcribeFileWithMel(nativePtr, wavePath, mWhisperUtil.getFilters());
        Log.v("inference_get_mel", String.valueOf(System.currentTimeMillis()-time2));
        return value;
    }

    private String runInference(float[] inputData) {
        // Create input tensor
        Tensor inputTensor = mInterpreter.getInputTensor(0);
        TensorBuffer inputBuffer = TensorBuffer.createFixedSize(inputTensor.shape(), inputTensor.dataType());
        Log.d(TAG, "Input Tensor Dump ===>");
        printTensorDump(inputTensor);

        // Create output tensor
        Tensor outputTensor = mInterpreter.getOutputTensor(0);
        TensorBuffer outputBuffer = TensorBuffer.createFixedSize(outputTensor.shape(), DataType.FLOAT32);
        Log.d(TAG, "Output Tensor Dump ===>");
        printTensorDump(outputTensor);

        // Load input data
        int inputSize = inputTensor.shape()[0] * inputTensor.shape()[1] * inputTensor.shape()[2] * Float.BYTES;
        ByteBuffer inputBuf = ByteBuffer.allocateDirect(inputSize);
        inputBuf.order(ByteOrder.nativeOrder());
        for (float input : inputData) {
            inputBuf.putFloat(input);
        }

        // To test mel data as a input directly
//        try {
//            byte[] bytes = Files.readAllBytes(Paths.get("/data/user/0/com.example.tfliteaudio/files/mel_spectrogram.bin"));
//            inputBuf = ByteBuffer.wrap(bytes);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }

        inputBuffer.loadBuffer(inputBuf);

        // Run inference
        mInterpreter.run(inputBuffer.getBuffer(), outputBuffer.getBuffer());

        // Retrieve the results
        int outputLen = outputBuffer.getIntArray().length;
        Log.d(TAG, "output_len: " + outputLen);
        StringBuilder result = new StringBuilder();
        long time = System.currentTimeMillis();
        for (int i = 0; i < outputLen; i++) {
            int token = outputBuffer.getBuffer().getInt();
            if (token == mWhisperUtil.getTokenEOT())
                break;

            // Get word for token and Skip additional token
            if (token < mWhisperUtil.getTokenEOT()) {
                String word = mWhisperUtil.getWordFromToken(token);
                Log.d(TAG, "Adding token: " + token + ", word: " + word);
                result.append(word);
            } else {
                if (token == mWhisperUtil.getTokenTranscribe())
                    Log.d(TAG, "It is Transcription...");

                if (token == mWhisperUtil.getTokenTranslate())
                    Log.d(TAG, "It is Translation...");

                String word = mWhisperUtil.getWordFromToken(token);
                Log.d(TAG, "Skipping token: " + token + ", word: " + word);
            }
        }
        Log.v("inference_time_decode", String.valueOf(System.currentTimeMillis()-time));

        return result.toString();
    }

    private void printTensorDump(Tensor tensor) {
        Log.d(TAG, "  shape.length: " + tensor.shape().length);
        for (int i = 0; i < tensor.shape().length; i++)
            Log.d(TAG, "    shape[" + i + "]: " + tensor.shape()[i]);
        Log.d(TAG, "  dataType: " + tensor.dataType());
        Log.d(TAG, "  name: " + tensor.name());
        Log.d(TAG, "  numBytes: " + tensor.numBytes());
        Log.d(TAG, "  index: " + tensor.index());
        Log.d(TAG, "  numDimensions: " + tensor.numDimensions());
        Log.d(TAG, "  numElements: " + tensor.numElements());
        Log.d(TAG, "  shapeSignature.length: " + tensor.shapeSignature().length);
        Log.d(TAG, "  quantizationParams.getScale: " + tensor.quantizationParams().getScale());
        Log.d(TAG, "  quantizationParams.getZeroPoint: " + tensor.quantizationParams().getZeroPoint());
        Log.d(TAG, "==================================================================");
    }

    static {
        System.loadLibrary("talkandexecute");
    }

    // Native methods
    private native long createEngine();
    private native float[] transcribeFileWithMel(long nativePtr, String waveFile, float[] filters);
}
