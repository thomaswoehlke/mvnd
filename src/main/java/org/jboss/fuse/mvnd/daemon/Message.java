/*
 * Copyright 2019 the original author or authors.
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
package org.jboss.fuse.mvnd.daemon;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class Message {

    final long timestamp = System.nanoTime();

    long timestamp() {
        return timestamp;
    }

    public static class BuildRequest extends Message {
        final List<String> args;
        final String workingDir;
        final String projectDir;

        public BuildRequest(List<String> args, String workingDir, String projectDir) {
            this.args = args;
            this.workingDir = workingDir;
            this.projectDir = projectDir;
        }

        public List<String> getArgs() {
            return args;
        }

        public String getWorkingDir() {
            return workingDir;
        }

        public String getProjectDir() {
            return projectDir;
        }
    }

    public static class BuildEvent extends Message {
        enum Type {
            BuildStarted, BuildStopped, ProjectStarted, ProjectStopped, MojoStarted, MojoStopped
        }
        final Type type;
        final String projectId;
        final String display;

        public BuildEvent(Type type, String projectId, String display) {
            this.type = type;
            this.projectId = projectId;
            this.display = display;
        }

        public Type getType() {
            return type;
        }

        public String getProjectId() {
            return projectId;
        }

        public String getDisplay() {
            return display;
        }

        @Override
        public String toString() {
            return "BuildEvent{" +
                    "type=" + type +
                    ", display='" + display + '\'' +
                    '}';
        }
    }

    public static class BuildMessage extends Message {
        final String message;

        public BuildMessage(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "BuildMessage{" +
                    "message='" + message + '\'' +
                    '}';
        }
    }

    public static class MessageSerializer implements Serializer<Message> {

        final int BUILD_REQUEST = 0;
        final int BUILD_EVENT = 1;
        final int BUILD_MESSAGE = 2;

        @Override
        public Message read(DataInputStream input) throws EOFException, Exception {
            int type = input.read();
            if (type == -1) {
                return null;
            }
            switch (type) {
                case BUILD_REQUEST:
                    return readBuildRequest(input);
                case BUILD_EVENT:
                    return readBuildEvent(input);
                case BUILD_MESSAGE:
                    return readBuildMessage(input);
            }
            throw new IllegalStateException("Unexpected message type: " + type);
        }

        @Override
        public void write(DataOutputStream output, Message value) throws Exception {
            if (value instanceof BuildRequest) {
                output.write(BUILD_REQUEST);
                writeBuildRequest(output, (BuildRequest) value);
            } else if (value instanceof BuildEvent) {
                output.write(BUILD_EVENT);
                writeBuildEvent(output, (BuildEvent) value);
            } else if (value instanceof BuildMessage) {
                output.write(BUILD_MESSAGE);
                writeBuildMessage(output, (BuildMessage) value);
            } else {
                throw new IllegalStateException();
            }
        }

        private BuildRequest readBuildRequest(DataInputStream input) throws IOException {
            List<String> args = readStringList(input);
            String workingDir = input.readUTF();
            String projectDir = input.readUTF();
            return new BuildRequest(args, workingDir, projectDir);
        }

        private void writeBuildRequest(DataOutputStream output, BuildRequest value) throws IOException {
            writeStringList(output, value.args);
            output.writeUTF(value.workingDir);
            output.writeUTF(value.projectDir);
        }

        private BuildEvent readBuildEvent(DataInputStream input) throws IOException {
            BuildEvent.Type type = BuildEvent.Type.values()[input.read()];
            String projectId = input.readUTF();
            String display = input.readUTF();
            return new BuildEvent(type, projectId, display);
        }

        private void writeBuildEvent(DataOutputStream output, BuildEvent value) throws IOException {
            output.write(value.type.ordinal());
            output.writeUTF(value.projectId);
            output.writeUTF(value.display);
        }

        private BuildMessage readBuildMessage(DataInputStream input) throws IOException {
            String message = input.readUTF();
            return new BuildMessage(message);
        }

        private void writeBuildMessage(DataOutputStream output, BuildMessage value) throws IOException {
            output.writeUTF(value.message);
        }

        private List<String> readStringList(DataInputStream input) throws IOException {
            ArrayList<String> l = new ArrayList<>();
            int nb = input.readInt();
            for (int i = 0; i < nb; i++) {
                l.add(input.readUTF());
            }
            return l;
        }

        private void writeStringList(DataOutputStream output, List<String> value) throws IOException {
            output.writeInt(value.size());
            for (String v : value) {
                output.writeUTF(v);
            }
        }

    }
}