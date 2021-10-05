import librosa
import librosa.display
import numpy as np


def normalize_signal_length(
        source_signal: np.ndarray,
        target_length_sec: int = 2,
        sample_rate: int = 16000,
        expand_random: bool = True,
) -> np.ndarray:
    """
    Standardizes the signal length so that all samples are the same length.

    Parameters
    ----------
    source_signal : np.ndarray
        Source signal in numpy array format.

    target_length_sec : int
        Target sample time in seconds

    sample_rate : int
        Sample rate

    expand_random: bool
        Flag denoting whether we should expand the signal with random values. if False - fill with zeros

    Returns
    -------
    np.ndarray
        Signal of specified length

    """
    source_signal_length = len(source_signal)
    target_length_samples = target_length_sec * sample_rate

    if source_signal_length == target_length_samples:
        return source_signal

    elif source_signal_length // sample_rate < target_length_sec:
        if expand_random:
            result_signal = np.random.normal(scale=0.01, size=target_length_samples)
        else:
            result_signal = np.zeros(target_length_samples)

        random_offset = np.random.randint(len(result_signal) - source_signal_length)
        result_signal[random_offset: random_offset + source_signal_length] = source_signal[:]
        return result_signal

    else:
        random_offset = np.random.randint(source_signal_length - target_length_samples)
        return source_signal[random_offset: random_offset + target_length_samples]


def generate_spectrogram(
        signal: np.ndarray,
        sample_rate: int = 16000,
        n_fft: int = 1024,
        hop_length: int = 128,
        mean: int = -38.598,
        std: int = 12.22,
) -> np.ndarray:
    """
    Generates a spectrogram from an audio signal.

    Parameters
    ----------
    signal : np.ndarray
        1D signal in numpy array format.

    sample_rate: int
        Sample rate

    n_fft : int
        Length of the FFT window

    hop_length : int
        Number of steps between FFT windows

    mean: int
        Mean value of spectrogram

    std: int
        Std value of spectrogram

    Returns
    -------
    np.ndarray
        Spectrogram for the input signal

    """
    mel_spect = librosa.feature.melspectrogram(
        y=signal,
        sr=sample_rate,
        n_fft=n_fft,
        hop_length=hop_length,
    )
    mel_spect = librosa.power_to_db(mel_spect, ref=np.max)
    normalized_mel_spec = (mel_spect - mean) / std

    return normalized_mel_spec


def mix_two_signals(
        main_signal: np.ndarray,
        background_signal: np.ndarray,
        target_length_sec: int = 2,
        sample_rate: int = 16000,
) -> np.ndarray:
    """
    Mixes two signals. Inserts background noise below the main signal.

    Parameters
    ----------
    main_signal : np.ndarray
        The main signal in numpy array format to be mixed

    background_signal : np.ndarray
        Background signal in numpy array format

    target_length_sec : int
        Target sample time in seconds

    sample_rate : int
        Sample rate

    Returns
    -------
    np.ndarray
        Mix of two signals

    """

    main_signal = normalize_signal_length(
        main_signal,
        target_length_sec=target_length_sec,
        sample_rate=sample_rate,
        expand_random=False,
    )
    background_signal = normalize_signal_length(
        background_signal,
        target_length_sec=target_length_sec,
        sample_rate=sample_rate,
    )

    normalized_main_signal = main_signal / (np.max(main_signal) - np.min(main_signal))
    scaled_main_signal = normalized_main_signal * (np.max(background_signal) - np.min(background_signal))
    alpha = 0.8
    mixed_signal = scaled_main_signal * alpha + background_signal * (1 - alpha)
    return mixed_signal
