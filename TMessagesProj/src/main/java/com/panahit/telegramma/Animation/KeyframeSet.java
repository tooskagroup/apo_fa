/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.panahit.telegramma.Animation;

import java.util.ArrayList;
import java.util.Arrays;
import android.util.Log;
import android.view.animation.Interpolator;

class KeyframeSet {

    int mNumKeyframes;

    Keyframe mFirstKeyframe;
    Keyframe mLastKeyframe;
    Interpolator mInterpolator;
    ArrayList<Keyframe> mKeyframes;
    TypeEvaluator mEvaluator;

    public KeyframeSet(Keyframe... keyframes) {
        mNumKeyframes = keyframes.length;
        mKeyframes = new ArrayList<Keyframe>();
        mKeyframes.addAll(Arrays.asList(keyframes));
        mFirstKeyframe = mKeyframes.get(0);
        mLastKeyframe = mKeyframes.get(mNumKeyframes - 1);
        mInterpolator = mLastKeyframe.getInterpolator();
    }

    public static KeyframeSet ofInt(int... values) {
        int numKeyframes = values.length;
        Keyframe.IntKeyframe keyframes[] = new Keyframe.IntKeyframe[Math.max(numKeyframes,2)];
        if (numKeyframes == 1) {
            keyframes[0] = (Keyframe.IntKeyframe) Keyframe.ofInt(0f);
            keyframes[1] = (Keyframe.IntKeyframe) Keyframe.ofInt(1f, values[0]);
        } else {
            keyframes[0] = (Keyframe.IntKeyframe) Keyframe.ofInt(0f, values[0]);
            for (int i = 1; i < numKeyframes; ++i) {
                keyframes[i] = (Keyframe.IntKeyframe) Keyframe.ofInt((float) i / (numKeyframes - 1), values[i]);
            }
        }
        return new IntKeyframeSet(keyframes);
    }

    public static KeyframeSet ofFloat(float... values) {
        boolean badValue = false;
        int numKeyframes = values.length;
        Keyframe.FloatKeyframe keyframes[] = new Keyframe.FloatKeyframe[Math.max(numKeyframes,2)];
        if (numKeyframes == 1) {
            keyframes[0] = (Keyframe.FloatKeyframe) Keyframe.ofFloat(0f);
            keyframes[1] = (Keyframe.FloatKeyframe) Keyframe.ofFloat(1f, values[0]);
            if (Float.isNaN(values[0])) {
                badValue = true;
            }
        } else {
            keyframes[0] = (Keyframe.FloatKeyframe) Keyframe.ofFloat(0f, values[0]);
            for (int i = 1; i < numKeyframes; ++i) {
                keyframes[i] = (Keyframe.FloatKeyframe) Keyframe.ofFloat((float) i / (numKeyframes - 1), values[i]);
                if (Float.isNaN(values[i])) {
                    badValue = true;
                }
            }
        }
        if (badValue) {
            Log.w("Animator", "Bad value (NaN) in float animator");
        }
        return new FloatKeyframeSet(keyframes);
    }

    public static KeyframeSet ofKeyframe(Keyframe... keyframes) {
        int numKeyframes = keyframes.length;
        boolean hasFloat = false;
        boolean hasInt = false;
        boolean hasOther = false;
        for (Keyframe keyframe : keyframes) {
            if (keyframe instanceof Keyframe.FloatKeyframe) {
                hasFloat = true;
            } else if (keyframe instanceof Keyframe.IntKeyframe) {
                hasInt = true;
            } else {
                hasOther = true;
            }
        }
        if (hasFloat && !hasInt && !hasOther) {
            Keyframe.FloatKeyframe floatKeyframes[] = new Keyframe.FloatKeyframe[numKeyframes];
            for (int i = 0; i < numKeyframes; ++i) {
                floatKeyframes[i] = (Keyframe.FloatKeyframe) keyframes[i];
            }
            return new FloatKeyframeSet(floatKeyframes);
        } else if (hasInt && !hasFloat && !hasOther) {
            Keyframe.IntKeyframe intKeyframes[] = new Keyframe.IntKeyframe[numKeyframes];
            for (int i = 0; i < numKeyframes; ++i) {
                intKeyframes[i] = (Keyframe.IntKeyframe) keyframes[i];
            }
            return new IntKeyframeSet(intKeyframes);
        } else {
            return new KeyframeSet(keyframes);
        }
    }

    public static KeyframeSet ofObject(Object... values) {
        int numKeyframes = values.length;
        Keyframe.ObjectKeyframe keyframes[] = new Keyframe.ObjectKeyframe[Math.max(numKeyframes,2)];
        if (numKeyframes == 1) {
            keyframes[0] = (Keyframe.ObjectKeyframe) Keyframe.ofObject(0f);
            keyframes[1] = (Keyframe.ObjectKeyframe) Keyframe.ofObject(1f, values[0]);
        } else {
            keyframes[0] = (Keyframe.ObjectKeyframe) Keyframe.ofObject(0f, values[0]);
            for (int i = 1; i < numKeyframes; ++i) {
                keyframes[i] = (Keyframe.ObjectKeyframe) Keyframe.ofObject((float) i / (numKeyframes - 1), values[i]);
            }
        }
        return new KeyframeSet(keyframes);
    }

    public void setEvaluator(TypeEvaluator evaluator) {
        mEvaluator = evaluator;
    }

    @Override
    public KeyframeSet clone() {
        ArrayList<Keyframe> keyframes = mKeyframes;
        int numKeyframes = mKeyframes.size();
        Keyframe[] newKeyframes = new Keyframe[numKeyframes];
        for (int i = 0; i < numKeyframes; ++i) {
            newKeyframes[i] = keyframes.get(i).clone();
        }
        return new KeyframeSet(newKeyframes);
    }

    @SuppressWarnings("unchecked")
    public Object getValue(float fraction) {
        if (mNumKeyframes == 2) {
            if (mInterpolator != null) {
                fraction = mInterpolator.getInterpolation(fraction);
            }
            return mEvaluator.evaluate(fraction, mFirstKeyframe.getValue(), mLastKeyframe.getValue());
        }
        if (fraction <= 0f) {
            final Keyframe nextKeyframe = mKeyframes.get(1);
            final Interpolator interpolator = nextKeyframe.getInterpolator();
            if (interpolator != null) {
                fraction = interpolator.getInterpolation(fraction);
            }
            final float prevFraction = mFirstKeyframe.getFraction();
            float intervalFraction = (fraction - prevFraction) / (nextKeyframe.getFraction() - prevFraction);
            return mEvaluator.evaluate(intervalFraction, mFirstKeyframe.getValue(), nextKeyframe.getValue());
        } else if (fraction >= 1f) {
            final Keyframe prevKeyframe = mKeyframes.get(mNumKeyframes - 2);
            final Interpolator interpolator = mLastKeyframe.getInterpolator();
            if (interpolator != null) {
                fraction = interpolator.getInterpolation(fraction);
            }
            final float prevFraction = prevKeyframe.getFraction();
            float intervalFraction = (fraction - prevFraction) / (mLastKeyframe.getFraction() - prevFraction);
            return mEvaluator.evaluate(intervalFraction, prevKeyframe.getValue(), mLastKeyframe.getValue());
        }
        Keyframe prevKeyframe = mFirstKeyframe;
        for (int i = 1; i < mNumKeyframes; ++i) {
            Keyframe nextKeyframe = mKeyframes.get(i);
            if (fraction < nextKeyframe.getFraction()) {
                final Interpolator interpolator = nextKeyframe.getInterpolator();
                if (interpolator != null) {
                    fraction = interpolator.getInterpolation(fraction);
                }
                final float prevFraction = prevKeyframe.getFraction();
                float intervalFraction = (fraction - prevFraction) / (nextKeyframe.getFraction() - prevFraction);
                return mEvaluator.evaluate(intervalFraction, prevKeyframe.getValue(), nextKeyframe.getValue());
            }
            prevKeyframe = nextKeyframe;
        }
        return mLastKeyframe.getValue();
    }

    @Override
    public String toString() {
        String returnVal = " ";
        for (int i = 0; i < mNumKeyframes; ++i) {
            returnVal += mKeyframes.get(i).getValue() + "  ";
        }
        return returnVal;
    }
}
