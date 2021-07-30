/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.tts

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.indexOfFirstWithHref
import org.readium.r2.testapp.R
import org.readium.r2.testapp.databinding.FragmentScreenReaderBinding
import org.readium.r2.testapp.reader.ReaderViewModel

class ScreenReaderFragment : Fragment(), ScreenReaderEngine.Listener {

    private lateinit var preferences: SharedPreferences

    private lateinit var publication: Publication

    private lateinit var screenReader: ScreenReaderEngine

    private var _binding: FragmentScreenReaderBinding? = null
    private val binding get() = _binding!!

    // A reference to the listener must be kept in order to prevent garbage collection
    // See https://developer.android.com/reference/android/content/SharedPreferences#registerOnSharedPreferenceChangeListener(android.content.SharedPreferences.OnSharedPreferenceChangeListener)
    private val preferencesListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "reader_TTS_speed") {
            updateScreenReaderSpeed(restart = true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activity = requireActivity()

        preferences = activity.getPreferences(Context.MODE_PRIVATE)

        ViewModelProvider(activity).get(ReaderViewModel::class.java).let {
            publication = it.publication
        }

        screenReader = ScreenReaderEngine(activity, publication)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScreenReaderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        screenReader.addListener(this)

        binding.titleView.text = publication.metadata.title

        binding.playPause.setOnClickListener {
            if (screenReader.isPaused) {
                screenReader.resumeReading()
            } else {
                screenReader.pauseReading()
            }
        }
        binding.fastForward.setOnClickListener {
            if (!screenReader.nextSentence()) {
                binding.nextChapter.callOnClick()
            }
        }
        binding.nextChapter.setOnClickListener {
            screenReader.nextResource()
        }

        binding.fastBack.setOnClickListener {
            if (!screenReader.previousSentence()) {
                binding.prevChapter.callOnClick()
            }
        }
        binding.prevChapter.setOnClickListener {
            screenReader.previousResource()
        }

        updateScreenReaderSpeed(restart = false)
        preferences.registerOnSharedPreferenceChangeListener(preferencesListener)

        val initialLocator = ScreenReaderContract.parseArguments(requireArguments()).locator
        val resourceIndex = requireNotNull(publication.readingOrder.indexOfFirstWithHref(initialLocator.href))
        screenReader.goTo(resourceIndex)
    }

    override fun onPlayStateChanged(playing: Boolean) {
        if (playing) {
            binding.playPause.setImageResource(R.drawable.ic_baseline_pause_24)
        } else {
            binding.playPause.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        }
    }

    override fun onEndReached() {
        Toast.makeText(requireActivity().applicationContext, "No further chapter contains text to read", Toast.LENGTH_LONG).show()
    }

    override fun onPlayTextChanged(text: String) {
        binding.ttsTextView.text = text
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(binding.ttsTextView, 1, 30, 1, TypedValue.COMPLEX_UNIT_DIP)
    }

    override fun onDestroyView() {
        screenReader.removeListener(this)
        _binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            screenReader.shutdown()
        } catch (e: Exception) {
        }
    }

    override fun onPause() {
        super.onPause()
        screenReader.pauseReading()
    }

    override fun onStop() {
        super.onStop()
        screenReader.stopReading()
        val result = ScreenReaderContract.createResult(screenReader.currentLocator)
        setFragmentResult(ScreenReaderContract.REQUEST_KEY, result)
    }

    private fun updateScreenReaderSpeed(restart: Boolean) {
        // Get user settings speed when opening the screen reader. Get a neutral percentage (corresponding to
        // the normal speech speed) if no user settings exist.
        val speed = preferences.getInt(
            "reader_TTS_speed",
            (2.75 * 3.toDouble() / 11.toDouble() * 100).toInt()
        )
        // Convert percentage to a float value between 0.25 and 3.0
        val ttsRate = 0.25.toFloat() + (speed.toFloat() / 100.toFloat()) * 2.75.toFloat()

        screenReader.setSpeechSpeed(ttsRate, restart)
    }
}
